package ch.unibas.cs.shm

import java.io.File

import scalismo.geometry._3D
import scalismo.io.{LandmarkIO, MeshIO}
import scalismo.registration.LandmarkRegistration
import scalismo.ui.api.SimpleAPI.ScalismoUI

/**
  * Created by George on 3/4/2017.
  */
object Launcher {
  def main(args: Array[String]): Unit = {

    scalismo.initialize()

    val ui = ScalismoUI()

    // For demo please use reduced amount of samples i.e. 5 or 10
    // Otherwise it will ate your RAM :)
    val meshes = new File("data/SMIR/meshes/").listFiles().take(50)
    val landmarks = new File("data/SMIR/landmarks/").listFiles().take(50)
    val refMesh = new File("data/femur.stl")
    val refLandmark = new File("data/femur.json")

    val meshesDataset = meshes.map{f: File => MeshIO.readMesh(f).get}
    val refMeshData = MeshIO.readMesh(refMesh).get

    // show reference femur bone
    ui.show(refMeshData, "femur_ref")


    // load landmarks, and add them to femurs
    val landmarksDataset = landmarks.map{f: File => LandmarkIO.readLandmarksJson[_3D](f).get}
    val refLandmarkData = LandmarkIO.readLandmarksJson[_3D](refLandmark).get

    val bestTransforms = (0 until 50).map{i: Int => LandmarkRegistration.rigid3DLandmarkRegistration(landmarksDataset(i), refLandmarkData)}
    val alignedFemurs = (0 until 50).map{i: Int => meshesDataset(i).transform(bestTransforms(i))}

    (0 until 50).foreach{i: Int => ui.show(alignedFemurs(i), "femur_"+i)}


  }
}
