package ch.unibas.cs.shm

import java.io.File

import scalismo.io.MeshIO
import scalismo.ui.api.SimpleAPI.ScalismoUI

/**
  * Created by George on 3/4/2017.
  */
object Launcher {
  def main(args: Array[String]): Unit = {

    scalismo.initialize()

    val ui = ScalismoUI()
    val mesh = MeshIO.readMesh(new File("data/facemesh.stl")).get
    val meshView = ui.show(mesh, "face")
  }
}
