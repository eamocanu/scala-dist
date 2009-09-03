/* SBaz -- Scala Bazaar
 * Copyright 2005-2009 LAMP/EPFL
 * @author  Lex Spoon
 */

// $Id$

package sbaz.download

import java.net.URL
import java.io.{File, FileOutputStream}
import Download._

/**
 * A single threaded class to manage downloads of files from the
 * Internet into a user-specified directory.
 */
class SimpleDownloader(val dir: File) extends Downloader {

  def download(url: URL, toname: String): FinalStatus = {
    dir.mkdirs() // make sure the cache directory exists

    val toFile = new File(dir, toname)
    if (toFile.exists) return Cached(toFile)  //TODO: Add force flag to not return cached downloads

    val tmpFile = new File(toFile.getAbsolutePath() + ".tmp")
    val f = new FileOutputStream(tmpFile)

    val con = url.openConnection()
    val inputStream = con.getInputStream()
    val contentLenStr = "/" + formatBytes(con.getContentLength())

    def lp(downloadedLen: Long, lastStatusLen: Int) {
      val dat = new Array[Byte](1024)
      val numread = inputStream.read(dat)
      if (numread >= 0) {
        f.write(dat, 0, numread)
        val newLen = downloadedLen + numread
        lp(newLen, printStatus(lastStatusLen, formatBytes(newLen) + contentLenStr)) 
      }
    }
    Console.println("Downloading: " + url.toString)
    lp(0l, 0)
    f.close()
    toFile.delete()
    tmpFile.renameTo(toFile)

    Console.println(" Done")
    Done(toFile)
  }

  def download[A <: DownloadType](dnl: A): FinalStatus = {
    download(dnl.link, dnl.filename)
  }

  def download[A <: DownloadType](downloads:List[A]): Map[A, FinalStatus] = {
    downloads.foldLeft[Map[A, FinalStatus]]( Map.empty ) { 
      (map, dnl) =>  map.updated(dnl, download(dnl.link, dnl.filename))
    }
  }

}
