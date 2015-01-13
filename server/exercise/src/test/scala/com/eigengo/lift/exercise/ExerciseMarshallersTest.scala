package com.eigengo.lift.exercise

import java.nio.ByteBuffer

import com.eigengo.lift.exercise.DeviceSensorData.MultipleDeviceSensorData.{Source, SingleDeviceSensorData}
import com.eigengo.lift.exercise.DeviceSensorData.MultipleDeviceSensorData
import com.eigengo.lift.exercise.packet.{RawSensorData, MultiPacket}
import com.google.protobuf.ByteString
import org.scalatest.{Matchers, FlatSpec}
import scodec.bits.BitVector
import spray.http.{HttpEntity, HttpRequest}

object ExerciseMarshallersTest {
  object TestData {
    def data() = {
      val dataBuffer = ByteBuffer.allocate(1);
      dataBuffer.put(5.toByte);
      dataBuffer.flip();

      ByteString.copyFrom(dataBuffer)
    }

    def protobufMessage(source: Source) =
      MultipleDeviceSensorData
        .newBuilder()
        .addSingleDeviceSensorData(SingleDeviceSensorData.newBuilder().setSource(source).setData(data()))
        .build
        .toByteArray

    def protobufMessageMultipleDevices(sources: Seq[Source]) = {
      val builder = MultipleDeviceSensorData.newBuilder()
      sources.foreach(s => builder.addSingleDeviceSensorData(SingleDeviceSensorData.newBuilder().setSource(s).setData(data())))

      builder
        .build
        .toByteArray
    }

    def multiPacket(source: SensorDataSourceLocation) =
      multiPacketMultipleDevices(List(source))

    def multiPacketMultipleDevices(sources: Seq[SensorDataSourceLocation]) =
      MultiPacket(sources.map(s => RawSensorData(s, BitVector(TestData.data().toByteArray))))
  }
}

class ExerciseMarshallersTest
  extends FlatSpec
  with Matchers
  with ExerciseMarshallers {

  import ExerciseMarshallersTest._

  val underTest = BitVectorFromRequestUnmarshaller

  "The marshaller" should "unmarshall protocol buffer message from wrist device" in {
    val message = TestData.protobufMessage(Source.WRIST)

    underTest.apply(HttpRequest(entity = HttpEntity(message))) should be(Right(TestData.multiPacket(SensorDataSourceLocationWrist)))
  }

  it should "unmarshall protocol buffer message from waist device" in {
    val message = TestData.protobufMessage(Source.WAIST)

    underTest.apply(HttpRequest(entity = HttpEntity(message))) should be(Right(TestData.multiPacket(SensorDataSourceLocationWaist)))
  }

  it should "unmarshall protocol buffer message from multiple devices" in {
    val message = TestData.protobufMessageMultipleDevices(List(Source.WAIST, Source.WRIST))

    underTest.apply(HttpRequest(entity = HttpEntity(message))) should be(Right(TestData.multiPacketMultipleDevices(List(SensorDataSourceLocationWaist, SensorDataSourceLocationWrist))))
  }
}
