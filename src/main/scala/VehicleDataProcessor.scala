import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import org.apache.spark.sql.expressions.UserDefinedFunction
import org.apache.spark.sql.functions.{col, _}
import org.apache.spark.sql.types.StringType
import org.apache.spark.sql.{SaveMode, SparkSession}

import scala.io._



object VehicleDataProcessor {

  var VEHICLE_ID = "vehicle_id"
  var SAMPLE_TIME = "sample_time"
  var SAMPLE_TIME_TRUNC = "sample_time_trunc"

  def main(args: Array[String]): Unit = {
    try {

      // read input parameters
      if (args.length <4) {
        println("Error: File name argument is required\n" +
          "Use: VehicleDataProcessor <num of partitions> <spark mode (use 'local' for local)> <input ile name> <output path> <configuration file>")
        System.exit(-2)
      }
      val numOfPartitions = args(0).toInt
      val mode = args(1)
      val inFilename = args(2)
      val outFilename = args(3)
      val fieldMapFilename = args(4)

      // read Json configuration file using  'fasterxml' package into MapConfig case class
      val json = Source.fromFile(fieldMapFilename)
      val mapper = new ObjectMapper() with ScalaObjectMapper
      mapper.registerModule(DefaultScalaModule)

      // convert case class into Map of input => output field
      val fieldMapping = mapper.readValue[List[MapConfig]](json.reader())
      val filteredFieldMapping = fieldMapping.filter(_.output_field_name != null).map(x => x)
      val aggregationFields = filteredFieldMapping.
        filter(x => !x.input_field_name.equals(VEHICLE_ID) && !x.input_field_name.equals(SAMPLE_TIME))
        .map(x => last(x.input_field_name).alias(x.input_field_name))

      // Build Spark session.
      // User will decide to run in local more - preferred for single machine or remote mode in cluster environment.

      println("==>mode" + mode)
      val spark = SparkSession.builder().master(mode).appName("VehicleDataProcessor").getOrCreate()
      // register UDF
      spark.udf.register("myUdf", ConversionUtils.truncateUdf)

      // read input CSV file
      println("==>" + inFilename)
      val folders = List(inFilename)
//      val df = spark.read.format("com.databricks.spark.csv").option("header", value = true).option("inferschema", value=true).load(inFilename)
      val df = spark.read.option("header", value = true).csv(folders: _*)
      df.printSchema()
      // calculate and add new field 'sample_time_trunc' that is 'sample_time' truncated with 10 seconds
      val df1 = df.withColumn(SAMPLE_TIME_TRUNC, ConversionUtils.truncateUdf(col(SAMPLE_TIME)).cast(StringType))
      df1.printSchema()
      // the following sentence implements grouping of records by vehicle id and sample_time truncated
      // In SQL il looks like:
      //    SELECT vehicle_id, sample_time_trunc, max(sample_time) as sample_time, last(speed) as speed, last(fuel_level) as fuel_level
      //           FROM df1 GROUP BY vehicle_id, sample_time_trunc
      val df2 = df1.groupBy(VEHICLE_ID, SAMPLE_TIME_TRUNC)
        .agg(
          max(SAMPLE_TIME).alias(SAMPLE_TIME),
          aggregationFields: _*
        )
      df2.printSchema()
      // rename/map fields and eliminate un-required fields
      val df3 = df2.select(filteredFieldMapping.map(x => col(x.input_field_name).alias(x.output_field_name)): _*)
      df3.printSchema()
      //write to disk in HDFS. number of partitions is given by parameters
      df3.repartition(numOfPartitions).write.format("org.apache.spark.sql.json").mode(SaveMode.Overwrite).save(outFilename)
    } catch {
      case e : Exception =>
        e.printStackTrace()
        System.exit(-1)
    }
    System.exit(0)

  }
}

/**
 * case calss that represents the configuration
 * @param input_field_name field from input file
 * @param output_field_name match field to output
 */
case class MapConfig(input_field_name: String, output_field_name: String)

/**
 * Udf function to truncate the time to 10 seconds
 */
object ConversionUtils  extends Serializable {
  def truncate10(s: String): String = ((s.toDouble/10).toLong *10).toString
  val truncateUdf: UserDefinedFunction = udf[String, String](truncate10)
}
