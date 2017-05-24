package p2

import java.io.File

import scalismo.common.PointId
import scalismo.geometry.{Landmark, Point, _3D}
import scalismo.io._
import scalismo.mesh.TriangleMesh
import scalismo.sampling.algorithms.MetropolisHastings
import scalismo.sampling.evaluators.ProductEvaluator
import scalismo.statisticalmodel.StatisticalMeshModel
import scalismo.statisticalmodel.asm.ActiveShapeModel
import scalismo.ui.api.SimpleAPI.ScalismoUI

import scala.collection.immutable

/**
  * Main working class for second project, that is a model-based segmentation
  * of 5 CT femur images using MCMC methods and Active Shape Models.
  *
  * Created by George on 20/5/2017.
  */
object P2 {

  def main(args: Array[String]) {
    scalismo.initialize()

    val ui = ScalismoUI()

    val asm: ActiveShapeModel = ActiveShapeModelIO.readActiveShapeModel(new File("handedData/femur-asm.h5")).get
    val image1 = ImageIO.read3DScalarImage[Short](new File("handedData/targets/1.nii")).get.map(_.toFloat)

    ui.show(asm.statisticalModel, "shapeModel")
    ui.show(asm.mean, "mean_intensities")

    ui.show(image1, "image1")

    val preProcessedGradientImage = asm.preprocessor(image1)

    val test1 = MeshIO.readMesh(new File("handedData/test/4.stl")).get
    ui.show(test1, "test_mesh")
    println("calculating likelihood value")
    println(likelihoodForMesh(asm, asm.statisticalModel.sample, preProcessedGradientImage))
  }


}
