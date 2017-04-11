package femur

import java.io.File

import scalismo.geometry._
import scalismo.io._
import scalismo.registration.LandmarkRegistration
import scalismo.geometry.Landmark


object RigidAlignment {

  def main(args: Array[String]) {

    scalismo.initialize()

    println("Loading models...")
    val files = new File("data/SMIR/meshes/").listFiles().take(50)
    val refFile = new File("data/femur.stl")
    val refLandmFile = new File("data/femur.json")
    val landmFiles = new File("data/SMIR/landmarks/").listFiles().take(50)


    val dataset = files.map { f: File => MeshIO.readMesh(f).get }
    val refData = MeshIO.readMesh(refFile).get

    println("Displaying femur bone...")
    //ui.show(refData, "femur_ref")


    println("Loading landmarks and adding them to femurs")
    val landmarks = landmFiles.map { f: File => LandmarkIO.readLandmarksJson[_3D](f).get }
    val refLandmark = LandmarkIO.readLandmarksJson[_3D](refLandmFile).get

    val bestTransforms = (0 until 50).map { i: Int => LandmarkRegistration.rigid3DLandmarkRegistration(landmarks(i), refLandmark) }

    println("Transforming landmarks")
    val alignedLandmarks = (bestTransforms zip landmarks).map { case (bTrans, landmarks) =>
      landmarks.indices.map { i: Int => Landmark("L_" + i, bTrans(landmarks(i).point)) }
    }

    val alignedFemurs = (0 until 50).map { i: Int => dataset(i).transform(bestTransforms(i)) }

    println("Start writing landmarks at `data/aligned/landmarks/`")
    (0 until 50).foreach { i: Int => LandmarkIO.writeLandmarksJson(alignedLandmarks(i), new File("data/aligned/landmarks/femur_" +i+".json")) }

    println("Start writing meshes at `data/aligned/meshes/`")
    (0 until 50).foreach { i: Int => MeshIO.writeSTL(alignedFemurs(i), new File("data/aligned/meshes/femur_" + i + ".stl")) }

    print("All done")
  }
}