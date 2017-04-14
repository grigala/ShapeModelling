package femur

import java.io.File

import scalismo.common.{DiscreteVectorField, PointId, UnstructuredPointsDomain}
import scalismo.geometry
import scalismo.geometry.{Point, SquareMatrix, Vector, _3D}
import scalismo.io.{MeshIO, StatismoIO}
import scalismo.mesh.TriangleMesh
import scalismo.statisticalmodel.{NDimensionalNormalDistribution, StatisticalMeshModel}
import scalismo.ui.api.SimpleAPI.ScalismoUI

/**
  * Created by George on 14/4/2017.
  */
object ShapeCompletion {

  def main(args: Array[String]): Unit = {
    scalismo.initialize()

    val ui = ScalismoUI()

    println("Loading and displaying partial mesh...")
    val partialShape: TriangleMesh = MeshIO.readMesh(new File("data/partials/VSD.Right_femur.XX.XX.OT.101147.0.stl")).get
    ui.show(partialShape, "partialShape")

    println("Loading and displaying statistical shape model...")
    val model: StatisticalMeshModel = StatismoIO.readStatismoMeshModel(new File("data/data_shape_model.h5")).get
    ui.show(model, "model")

    println("Getting correspondences for Gaussian Process regression...")
    val modelPts: Seq[Point[_3D]] = ui.getLandmarksOf("model").get.map { lm => lm.point }
    val partialShapePts: Seq[Point[_3D]] = ui.getLandmarksOf("partialShape").get.map { lm => lm.point }

    val domain: UnstructuredPointsDomain[_3D] = UnstructuredPointsDomain[_3D](modelPts.toIndexedSeq)
    val values: Seq[geometry.Vector[_3D]] = (modelPts zip partialShapePts).map { case (mPt, nPt) => nPt - mPt }

    println("Displaying observed shape deformation fields...")
    val field = DiscreteVectorField(domain, values.toIndexedSeq)
    ui.show(field, "observation")

    val noise = NDimensionalNormalDistribution(Vector(0, 0, 0), SquareMatrix((1f, 0, 0), (0, 1f, 0), (0, 0, 1f)))

    val trainingData: Seq[(PointId, Point[_3D], NDimensionalNormalDistribution[_3D])] =
      (modelPts zip partialShapePts).map { case (mPt, nPt) =>
        (model.mean.findClosestPoint(mPt).id, nPt, noise)
      }

    println("Getting a space of fitting deformations...")
    val posterior: StatisticalMeshModel = model.posterior(trainingData.toIndexedSeq)
    ui.show(posterior, "posterior")

    println("Done")
  }

}
