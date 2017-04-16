package femur

import java.io.File

import scalismo.common.{DiscreteVectorField, PointId, UnstructuredPointsDomain}
import scalismo.geometry._
import scalismo.io._
import scalismo.mesh.TriangleMesh
import scalismo.numerics.UniformMeshSampler3D
import scalismo.statisticalmodel._
import scalismo.ui.api.SimpleAPI.ScalismoUI


object FitFemur {

  def main(args: Array[String]) {

    val reconstructFemurNo = 1


    scalismo.initialize()

    ////////////////////SETTINGS FOR ICP
    val numIterations = 50
    val noise = NDimensionalNormalDistribution(Vector(0, 0, 0), SquareMatrix((1f, 0, 0), (0, 1f, 0), (0, 0, 1f)))

    //
    val ui = ScalismoUI()
    //
    println("Loading and displaying partial mesh...")
    val targetName: String = reconstructFemurNo match {
      case 1 => "data/partials/VSD.Right_femur.XX.XX.OT.101147.0.stl"
      case 2 => "data/partials/VSD.Right_femur.XX.XX.OT.101148.0.stl"
      case 3 => "data/partials/VSD.Right_femur.XX.XX.OT.101149.0.stl"
      case 4 => "data/partials/VSD.Right_femur.XX.XX.OT.101150.0.stl"
      case 5 => "data/partials/VSD.Right_femur.XX.XX.OT.101151.0.stl"
      case 6 => "data/partials/VSD.Right_femur.XX.XX.OT.101152.0.stl"
      case 7 => "data/partials/VSD.Right_femur.XX.XX.OT.101153.0.stl"
      case 8 => "data/partials/VSD.Right_femur.XX.XX.OT.101154.0.stl"
      case 9 => "data/partials/VSD.Right_femur.XX.XX.OT.101155.0.stl"
      case 10 => "data/partials/VSD.Right_femur.XX.XX.OT.101156.0.stl"
    }


    val target: TriangleMesh = MeshIO.readMesh(new File(targetName)).get
    ui.show(target, "partialShape")

    println("Loading and displaying statistical shape model...")
    val model: StatisticalMeshModel = StatismoIO.readStatismoMeshModel(new File("data/augmented_shape_model.h5")).get
    ui.show(model, "model")


    val pointSamples = UniformMeshSampler3D(model.mean, 5000, 42).sample.map(s => s._1)
    val pointIds = pointSamples.map { s => model.mean.findClosestPoint(s).id }


    val filteredPointIds = reconstructFemurNo match {
      case 1 => pointIds.filter { id: PointId => (model.referenceMesh.point(id) - Point3D(-40.558f, 26.1689f, -208.722f)).norm > 62 }
      case 2 => pointIds.filter { id: PointId => model.referenceMesh.point(id).z < 50.7622 }
      case 3 => pointIds.filter { id: PointId => (91.0552 * model.referenceMesh.point(id).x + 31.6728 * model.referenceMesh.point(id).y + 10.516 * model.referenceMesh.point(id).z - 2294.54 < -100) }
      case 4 => pointIds.filter { id: PointId => model.referenceMesh.point(id).z < -83.69 || model.referenceMesh.point(id).z > 93.922 }
      case 5 => pointIds.filter { id: PointId => model.referenceMesh.point(id).z > -30.639 }
      case 6 => pointIds.filter { id: PointId => model.referenceMesh.point(id).z < -136 || model.referenceMesh.point(id).z > 158 }
      case 7 => pointIds.filter { id: PointId => model.referenceMesh.point(id).z > -84 || model.referenceMesh.point(id).z < -172 }
      case 8 => pointIds.filter { id: PointId => model.referenceMesh.point(id).z > -134 }
      case 9 => pointIds.filter { id: PointId => model.referenceMesh.point(id).x < 3.5 && model.referenceMesh.point(id).y < 33.7 && model.referenceMesh.point(id).z > -169.9 }
      case 10 => pointIds.filter { id: PointId => model.referenceMesh.point(id).z < 180 }
    }

    ui.show(filteredPointIds.map { id => model.mean.point(id) }, "points")


    def attributeCorrespondences(pts: Seq[Point[_3D]]): Seq[Point[_3D]] = {
      pts.map { pt => target.findClosestPoint(pt).point }
    }

    def fitModel(pointIds: IndexedSeq[PointId], candidateCorresp: Seq[Point[_3D]]): TriangleMesh = {
      val trainingData = (pointIds zip candidateCorresp).map { case (mId, pPt) =>
        (mId, pPt, noise)
      }
      val posterior = model.posterior(trainingData.toIndexedSeq)
      posterior.mean
    }

    def recursion(currentPoints: Seq[Point[_3D]], nbIterations: Int): Seq[Point[_3D]] = {

      val candidates = attributeCorrespondences(currentPoints)
      val fit = fitModel(filteredPointIds, candidates)
      val newPoints = filteredPointIds.map(id => fit.point(id))
      if (nbIterations > 0) {
        //Thread.sleep(1000)

        recursion(newPoints, nbIterations - 1)
      } else {
        ui.remove("fit")
        ui.show(fit, "fit")
        newPoints
      }
    }


    println("Performing fitting recursion and calculating correspondences...")
    val fittedModelPoints = recursion(filteredPointIds.map(id => model.mean.point(id)), numIterations)
    val targetPoints = attributeCorrespondences(fittedModelPoints)
    val refPoints = filteredPointIds.map(id => model.mean.point(id))

    val domain = UnstructuredPointsDomain[_3D](refPoints.toIndexedSeq)
    val diff = refPoints.zip(targetPoints).map { case (mPt, pPt) => pPt - mPt }

    DiscreteVectorField(domain, diff.toIndexedSeq)


    print("done")

  }
}