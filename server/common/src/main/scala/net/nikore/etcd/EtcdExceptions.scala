package net.nikore.etcd

object EtcdExceptions {
  object Exception {
    def apply(msg: String, etype: String, errorCode: Int) = new EtcdException {
      def message = msg

      def exceptionType = etype

      def code = errorCode
    }
  }

  trait EtcdException extends RuntimeException {
    def message: String

    def exceptionType: String

    def code: Int


    override def toString = {
      this.getClass.getName + "("
      "message: " + message + ", " +
        "exceptionType: " + exceptionType + ", " +
        "code: " + code + ")"
    }
  }

  case class KeyNotFoundException(message: String, exceptionType: String, code: Int) extends EtcdException
}
