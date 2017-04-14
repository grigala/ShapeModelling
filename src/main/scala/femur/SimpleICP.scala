package femur

import java.io.File

import scalismo.common._
import scalismo.geometry._
import scalismo.io._
import scalismo.statisticalmodel._
import scalismo.ui.api.SimpleAPI.ScalismoUI

object SimpleICP {

  def main(args: Array[String]) {

    // required to initialize native libraries (VTK, HDF5 ..)
    scalismo.initialize()

    ////////////////////SETTINGS FOR ICP
    val numIterations = 15
    val noise = NDimensionalNormalDistribution(Vector(0, 0, 0), SquareMatrix((1f, 0, 0), (0, 1f, 0), (0, 0, 1f)))

    // create a visualization window
    val ui = ScalismoUI()

    /////////////////////Load Files
    val files = new File("data/aligned/meshes/").listFiles().take(50)
    val refFile = new File("data/femur.stl")
    val refLandmFile = new File("data/femur.json")
    val landmFiles = new File("data/aligned/landmarks/").listFiles().take(50)
    val referenceShapeModel = new File("data/reference_shape_model.h5")
    /////////////////////////////////////////////////////////////////

    val model = StatismoIO.readStatismoMeshModel(referenceShapeModel).get
    ui.show(model, "model")

    val target = MeshIO.readMesh(new File("data/aligned/meshes/femur_0.stl")).get
    //ui.show(target,"target")

    //model: StatisticalMeshModel, target: TriangleMesh, noise:
    //   NDimensionalNormalDistribution[_3D], numIterations: Int)
    val transformationField: DiscreteVectorField[_3D, _3D] =
    calcCorrespondentTransformationsICP(model, target, noise, numIterations)


    ui.show(transformationField, "deformations")


    //println(pointIds.take(10))
    //println(targetIDs.take(10))
    //println(pointIds.length)
    //println(targetIDs.length)

    //val refPoints = targetIDs.slice(5,6).map{id => model.mean.point(id)}
    //val targetPoints = targetIDs.slice(5,6).map{id => target.point(id)}
    //ui.show(refPoints, "refPoints")
    //ui.show(targetPoints, "targetPoints")
    print("done")
  }
}