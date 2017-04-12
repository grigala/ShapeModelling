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
    ////////////////////SETTINGS FOR ICP
    val numIterations = 10
    val noise = NDimensionalNormalDistribution(Vector(0,0,0), SquareMatrix((1f,0,0), (0,1f,0), (0,0,1f)))

    // required to initialize native libraries (VTK, HDF5 ..)
    scalismo.initialize()

    // create a visualization window
    val ui = ScalismoUI()

    /////////////////////Load Files
    val files = new File("data/aligned/meshes/").listFiles().take(50)
    val refFile = new File("data/femur.stl")
    val refLandmFile = new File("data/femur.json")
    val landmFiles = new File("data/aligned/landmarks/").listFiles().take(50)
    /////////////////////////////////////////////////////////////////

    val refFemur = MeshIO.readMesh(refFile).get

    val zeroMean = VectorField(RealSpace[_3D], (pt:Point[_3D]) => Vector(0,0,0))

    val l = 80
    val scalarValuedKernel = GaussianKernel[_3D](l) * 1
    val s = Array[Double](20, 20, 250)
    val matrixValuedKernel = DiagonalKernel(scalarValuedKernel * s(0), scalarValuedKernel * s(1), scalarValuedKernel * s(2))

    val gp = GaussianProcess(zeroMean, matrixValuedKernel)
    val sampler = RandomMeshSampler3D(refFemur, 400, 42)
    val lowRankGP : LowRankGaussianProcess[_3D, _3D] = LowRankGaussianProcess.approximateGP(gp, sampler, 40)
    val model = StatisticalMeshModel(refFemur, lowRankGP)
    ui.show(model, "model")

    val target = MeshIO.readMesh(new File("data/aligned/meshes/femur_0.stl")).get
    //val targetSampler = RandomMeshSampler3D(target, 500, 42)
    ui.show(target,"target")

    val pointSamples = UniformMeshSampler3D(model.mean, 5000, 42).sample.map(s => s._1)
    val pointIds = pointSamples.map{s => model.mean.findClosestPoint(s).id}

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

    def recursion(currentPoints : Seq[Point[_3D]], nbIterations : Int) : Unit= {

      val candidates = attributeCorrespondences(currentPoints)
      val fit = fitModel(pointIds, candidates)
      ui.remove("fit")
      ui.show(fit,"fit")

      val newPoints= pointIds.map(id => fit.point(id))

      if(nbIterations> 0) {
        Thread.sleep(3000)
        recursion(newPoints, nbIterations - 1)
      }
    }

    recursion( pointIds.map(id => model.mean.point(id)) , numIterations)

    print("done")
  }
}