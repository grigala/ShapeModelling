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

    val meshes = new File("data/SMIR/meshes/").listFiles().take(10)
    val refMesh = new File("data/femur.stl")
    val refLandmark = new File("data/femur.json")
    val landmarks = new File("data/SMIR/landmarks/").listFiles().take(10)

    val meshesDataset = meshes.map{f: File => MeshIO.readMesh(f).get}
    val refMeshData = MeshIO.readMesh(refMesh).get
    // show ref femur bone
    ui.show(refMeshData, "femur_ref")


    // load landmarks, add them to femurs
    val landmarksDataset = landmarks.map{f: File => LandmarkIO.readLandmarksJson[_3D](f).get}
    val refLandmarkData = LandmarkIO.readLandmarksJson[_3D](refLandmark).get

    val bestTransforms = (0 until 10).map{i: Int => LandmarkRegistration.rigid3DLandmarkRegistration(landmarksDataset(i), refLandmarkData)}
    val alignedFemurs = (0 until 10).map{i: Int => meshesDataset(i).transform(bestTransforms(i))}

    (0 until 10).foreach{i: Int => ui.show(alignedFemurs(i), "femur_"+i)}


  }
}
