import java.io.File

import scalismo.geometry._3D
import scalismo.io.{LandmarkIO, MeshIO, StatismoIO}
import scalismo.mesh.TriangleMesh
import scalismo.statisticalmodel.StatisticalMeshModel
import scalismo.ui.api.SimpleAPI.ScalismoUI

/**
  * Created by Fabricio on 14.04.2017.
  */

object ViewPartialFemurs {
  def main(args: Array[String]) {

    scalismo.initialize()

    val ui = ScalismoUI()

    val modelLandmarks = LandmarkIO.readLandmarksJson[_3D](new File("data/femur.json")).get
    println("Loading and displaying partial mesh...")
    val partialShape0: TriangleMesh = MeshIO.readMesh(new File("data/partials/VSD.Right_femur.XX.XX.OT.101147.0.stl")).get
    val partialShape1: TriangleMesh = MeshIO.readMesh(new File("data/partials/VSD.Right_femur.XX.XX.OT.101148.0.stl")).get
    val partialShape2: TriangleMesh = MeshIO.readMesh(new File("data/partials/VSD.Right_femur.XX.XX.OT.101149.0.stl")).get
    val partialShape3: TriangleMesh = MeshIO.readMesh(new File("data/partials/VSD.Right_femur.XX.XX.OT.101150.0.stl")).get
    val partialShape4: TriangleMesh = MeshIO.readMesh(new File("data/partials/VSD.Right_femur.XX.XX.OT.101151.0.stl")).get
    val partialShape5: TriangleMesh = MeshIO.readMesh(new File("data/partials/VSD.Right_femur.XX.XX.OT.101152.0.stl")).get
    val partialShape6: TriangleMesh = MeshIO.readMesh(new File("data/partials/VSD.Right_femur.XX.XX.OT.101153.0.stl")).get
    val partialShape7: TriangleMesh = MeshIO.readMesh(new File("data/partials/VSD.Right_femur.XX.XX.OT.101154.0.stl")).get
    val partialShape8: TriangleMesh = MeshIO.readMesh(new File("data/partials/VSD.Right_femur.XX.XX.OT.101155.0.stl")).get
    val partialShape9: TriangleMesh = MeshIO.readMesh(new File("data/partials/VSD.Right_femur.XX.XX.OT.101156.0.stl")).get
    ui.show(partialShape0, "partialShape0")
    ui.show(partialShape1, "partialShape1")
    ui.show(partialShape2, "partialShape2")
    ui.show(partialShape3, "partialShape3")
    ui.show(partialShape4, "partialShape4")
    ui.show(partialShape5, "partialShape5")
    ui.show(partialShape6, "partialShape6")
    ui.show(partialShape7, "partialShape7")
    ui.show(partialShape8, "partialShape8")
    ui.show(partialShape9, "partialShape9")

    println("Loading and displaying statistical shape model...")
    val model: StatisticalMeshModel = StatismoIO.readStatismoMeshModel(new File("data/data_shape_model.h5")).get

    ui.show(model, "model")

    ui.addLandmarksTo(modelLandmarks, "model")
  }
}