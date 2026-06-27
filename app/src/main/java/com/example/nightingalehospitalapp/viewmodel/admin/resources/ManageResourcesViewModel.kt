package com.example.nightingalehospitalapp.viewmodel.admin.resources

import androidx.lifecycle.ViewModel
import com.example.nightingalehospitalapp.database.FirebaseConfig
import com.example.nightingalehospitalapp.models.hospital.Bed
import com.example.nightingalehospitalapp.models.hospital.Department
import com.example.nightingalehospitalapp.models.hospital.OperationTheatre
import com.example.nightingalehospitalapp.models.diagnostic.DiagnosticTest
import com.example.nightingalehospitalapp.repository.bed.BedRepository
import com.example.nightingalehospitalapp.repository.diagnostic.DiagnosticRepository
import com.example.nightingalehospitalapp.repository.surgery.SurgeryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ManageResourcesViewModel(
    private val bedRepository: BedRepository = BedRepository(),
    private val diagnosticRepository: DiagnosticRepository = DiagnosticRepository(),
    private val surgeryRepository: SurgeryRepository = SurgeryRepository()
) : ViewModel() {

    private val _beds = MutableStateFlow<List<Bed>>(emptyList())
    val beds: StateFlow<List<Bed>> = _beds
    
    private val _theatres = MutableStateFlow<List<OperationTheatre>>(emptyList())
    val theatres: StateFlow<List<OperationTheatre>> = _theatres
    
    private val _tests = MutableStateFlow<List<DiagnosticTest>>(emptyList())
    val tests: StateFlow<List<DiagnosticTest>> = _tests

    private val _departments = MutableStateFlow<List<Department>>(emptyList())
    val departments: StateFlow<List<Department>> = _departments

    init {
        FirebaseConfig.bedsRef.addSnapshotListener { snapshot, _ ->
            if (snapshot != null) _beds.value = snapshot.toObjects(Bed::class.java)
        }
        FirebaseConfig.operationTheatresRef.addSnapshotListener { snapshot, _ ->
            if (snapshot != null) _theatres.value = snapshot.toObjects(OperationTheatre::class.java)
        }
        FirebaseConfig.diagnosticTestsRef.addSnapshotListener { snapshot, _ ->
            if (snapshot != null) _tests.value = snapshot.toObjects(DiagnosticTest::class.java)
        }
        FirebaseConfig.departmentsRef.addSnapshotListener { snapshot, _ ->
            if (snapshot != null) _departments.value = snapshot.toObjects(Department::class.java)
        }
    }

    fun addBed(bed: Bed) = bedRepository.addBed(bed)
    fun updateBed(bed: Bed) = bedRepository.updateBed(bed)
    fun removeBed(bedId: String) = bedRepository.removeBed(bedId)

    fun addOperationTheatre(ot: OperationTheatre) = surgeryRepository.addOperationTheatre(ot)
    fun updateOperationTheatre(ot: OperationTheatre) = surgeryRepository.updateOperationTheatre(ot)
    fun removeOperationTheatre(otId: String) = surgeryRepository.removeOperationTheatre(otId)

    fun addDiagnosticTest(test: DiagnosticTest) = diagnosticRepository.addTest(test)
    fun updateDiagnosticTest(test: DiagnosticTest) = diagnosticRepository.updateTest(test)
    fun removeDiagnosticTest(testId: String) = diagnosticRepository.removeTest(testId)

    fun addDepartment(department: Department) {
        val id = FirebaseConfig.departmentsRef.document().id ?: return
        val updatedDept = department.copy(departmentId = id)
        FirebaseConfig.departmentsRef.document(id).set(updatedDept)
    }
    fun removeDepartment(departmentId: String) = FirebaseConfig.departmentsRef.document(departmentId).delete()
}
