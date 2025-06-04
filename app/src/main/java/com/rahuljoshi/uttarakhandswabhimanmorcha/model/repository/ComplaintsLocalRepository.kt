package com.rahuljoshi.uttarakhandswabhimanmorcha.model.repository

import com.rahuljoshi.uttarakhandswabhimanmorcha.model.local.ComplaintDao
import com.rahuljoshi.uttarakhandswabhimanmorcha.model.local.ComplaintEntity
import javax.inject.Inject


class ComplaintLocalRepository @Inject constructor(
    private val complaintDao: ComplaintDao
) {

    // Insert a single complaint
    suspend fun insertComplaint(complaint: ComplaintEntity) {
        complaintDao.insertComplaint(complaint)
    }

    // Fetch all complaints
    suspend fun getAllComplaints(): List<ComplaintEntity> {
        return complaintDao.getAllComplaints()
    }

    //delete all complaints
    suspend fun deleteAllComplaints(){
        complaintDao.deleteAllComplaints()
    }
}
