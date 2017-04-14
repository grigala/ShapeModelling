package femur

import java.io.File

import scalismo.common._
import scalismo.geometry._
import scalismo.io._
import scalismo.kernels._
import scalismo.numerics._
import scalismo.statisticalmodel._
import scalismo.ui.api.SimpleAPI.ScalismoUI
import scalismo.mesh.{Mesh, TriangleMesh}
object SimpleICP {

  def main(args: Array[String]) {

    // required to initialize native libraries (VTK, HDF5 ..)
    scalismo.initialize()

    ////////////////////SETTINGS FOR ICP
    val numIterations = 15
    val noise = NDimensionalNormalDistribution(Vector(0,0,0), SquareMatrix((1f,0,0), (0,1f,0), (0,0,1f)))

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
    //val targetSampler = RandomMeshSampler3D(target, 500, 42)
    ui.show(target,"target")

    val pointSamples = UniformMeshSampler3D(model.mean, 12000, 42).sample.map(s => s._1)
    val pointIds = pointSamples.map{s => model.mean.findClosestPoint(s).id}
    //println(pointIds.take(10))

    def attributeCorrespondences(pts : Seq[Point[_3D]]) : Seq[Point[_3D]] = {
      pts.map{pt => target.findClosestPoint(pt).point}
    }

    def fitModel(pointIds: IndexedSeq[PointId],candidateCorresp: Seq[Point[_3D]]) :TriangleMesh = {
      val trainingData = (pointIds zip candidateCorresp).map{ case (mId, pPt) =>
        (mId, pPt, noise)
      }
      val posterior = model.posterior(trainingData.toIndexedSeq)
      posterior.mean
    }

    def recursion(currentPoints : Seq[Point[_3D]], nbIterations : Int) : Seq[Point[_3D]]= {

      val candidates = attributeCorrespondences(currentPoints)
      val fit = fitModel(pointIds, candidates)
      val newPoints = pointIds.map(id => fit.point(id))
      if(nbIterations> 0) {
        //Thread.sleep(3000)
        recursion(newPoints, nbIterations - 1)
      }else{
        ui.remove("fit")
        ui.show(fit,"fit")
        newPoints
      }
    }

    val newPoints = recursion( pointIds.map(id => model.mean.point(id)) , numIterations)
    val targetPoints = attributeCorrespondences(newPoints)
    val refPoints = pointIds.map(id => model.mean.point(id))

    val domain = UnstructuredPointsDomain[_3D](refPoints.toIndexedSeq)
    val values =  (refPoints.zip(targetPoints)).map{case (mPt, pPt) => pPt - mPt}
    val field = DiscreteVectorField(domain, values.toIndexedSeq)
    ui.show(field, "deformations")


    //println(pointIds.take(10))
    //println(targetIDs.take(10))
    //println(pointIds.length)
    //println(targetIDs.length)

    //val refPoints = targetIDs.slice(5,6).map{id => model.mean.point(id)}
    //val targetPoints = targetIDs.slice(5,6).map{id => target.point(id)}
    ui.show(refPoints, "refPoints")
    ui.show(targetPoints, "targetPoints")
    print("done")
  }
}