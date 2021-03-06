package com.gilazaria.subsearch.output

import com.gilazaria.subsearch.model.{Record, RecordType}
import com.gilazaria.subsearch.utils.File

import scala.collection.SortedSet
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class StandardOutput(private val file: Option[File], private val verbose: Boolean) extends Output {
  private var saveToFileFuture: Future[Unit] = Future(Unit)

  override def print(string: String): Unit =  {
    if (file.isDefined) {
      saveToFileFuture = saveToFileFuture.map {
        _ => file.get.write(string)
      }
    }
  }

  override def writingToFileFuture: Future[Unit] = {
    saveToFileFuture
  }

  override def printRecords(records: SortedSet[Record]) = {
    if (verbose) printRecordsVerbose(records)
    else printRecordsNormal(records)
  }

  protected def printRecordsVerbose(records: SortedSet[Record]) = {
    val lines: List[String] =
      records
        .map(_.name)
        .toList
        .flatMap {
          subdomain =>
            val subdomainRecords: SortedSet[Record] = records.filter(_.name == subdomain)
            val recordTypes: SortedSet[RecordType] = subdomainRecords.map(_.recordType)

            recordTypes.flatMap {
              recordType =>
                subdomainRecords.filter(_.recordType == recordType).map {
                  case Record(_, _, data) =>
                    val msg = formatRecordTypeAndSubdomainForPrinting(recordType, subdomain)

                    if (recordType.isOneOf("A", "AAAA", "CNAME", "NS", "SRV"))
                      s"$msg  ->  $data"
                    else if (recordType.stringValue == "MX")
                      s"$msg  @@  $data"
                    else
                      s"$msg  --  $data"
                }
            }
        }

    if (lines.nonEmpty)
      println(lines.mkString("\n"))
  }

  protected def formatRecordTypeAndSubdomainForPrinting(recordType: RecordType, subdomain: String): String =
    prependTime(f"${recordType.toString}%-7s:  $subdomain")

  protected def printRecordsNormal(records: SortedSet[Record]) = {
    val lines: List[String] =
      records
        .map(_.name)
        .toList
        .map(subdomain => (subdomain, records.filter(_.name == subdomain).map(_.recordType)))
        .map((data: (String, SortedSet[RecordType])) => s"${data._2.mkString(", ")}:  ${data._1}")

    if (lines.nonEmpty)
      printSuccess(lines.mkString("\n"))
  }
}

object StandardOutput {
  def create(fileOption: Option[File], verbose: Boolean): Option[StandardOutput] =
    if (fileOption.isDefined) Some(new StandardOutput(fileOption, verbose))
    else None
}
