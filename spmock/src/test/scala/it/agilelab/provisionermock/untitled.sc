import org.slf4j.{Logger, LoggerFactory}

val logger: Logger = LoggerFactory.getLogger(getClass.getName)


val message: String = "my message"

logger.error("Could not build the cloud provider")
logger.error(message)

logger.error(s"Could not build the cloud provider: {}", message)