package net.nikore.etcd

object EtcdJsonProtocol {

  //single key/values
  case class NodeResponse(key: String, value: Option[String], modifiedIndex: Int, createdIndex: Int)
  case class EtcdResponse(action: String, node: NodeResponse, prevNode: Option[NodeResponse])

  //for hanlding dirs
  case class NodeListElement(key: String, dir: Option[Boolean], value: Option[String], nodes: Option[List[NodeListElement]])
  case class EtcdListResponse(action: String, node: NodeListElement)

  //for handling error messages
  case class Error(errorCode: Int, message: String, cause: String, index: Int)

//  implicit val nodeResponseFormat = jsonFormat4(NodeResponse)
//  implicit val etcdResponseFormat = jsonFormat3(EtcdResponse)
//
//  implicit val nodeListElementFormat: JsonFormat[NodeListElement] = lazyFormat(jsonFormat4(NodeListElement))
//  implicit val etcdResponseListFormat = jsonFormat2(EtcdListResponse)
//
//  implicit val errorFormat = jsonFormat4(Error)
}
