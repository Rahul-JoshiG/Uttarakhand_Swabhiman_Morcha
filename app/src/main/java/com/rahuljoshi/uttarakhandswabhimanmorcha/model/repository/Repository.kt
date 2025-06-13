package com.rahuljoshi.uttarakhandswabhimanmorcha.model.repository

import android.util.Log
import com.google.api.client.http.FileContent
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.Permission
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.rahuljoshi.uttarakhandswabhimanmorcha.model.data.Complaint
import com.rahuljoshi.uttarakhandswabhimanmorcha.model.data.User
import com.rahuljoshi.uttarakhandswabhimanmorcha.services.GMailSender
import com.rahuljoshi.uttarakhandswabhimanmorcha.utils.Constant
import com.rahuljoshi.uttarakhandswabhimanmorcha.utils.PrivateData
import com.rahuljoshi.uttarakhandswabhimanmorcha.utils.ShardPref
import com.rahuljoshi.uttarakhandswabhimanmorcha.wrapper.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URLConnection
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

class Repository @Inject constructor(
    private val firestore: FirebaseFirestore,
    internal val auth: FirebaseAuth,
    private val driveService: Drive
) {

    fun currentUserId(): String? {
        return auth.currentUser?.uid
    }

    fun fetchCurrentUser(uid: String): Flow<Resource<User>> = callbackFlow {
        Log.d(TAG, "fetchCurrentUser: fetch user in $TAG for $uid")

        if (uid.isBlank()) {
            trySend(Resource.Error("Invalid user ID"))
            close() // Important to close the flow in case of early return
            return@callbackFlow
        }

        val listenerRegistration = firestore.collection(Constant.USERS).document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "fetchCurrentUser: error found $error in $TAG")
                    trySend(Resource.Error("Error fetching user data: ${error.message}"))
                    return@addSnapshotListener
                }

                val user = snapshot?.toObject(User::class.java)
                if (user != null) {
                    Log.d(TAG, "fetchCurrentUser: in $TAG user = $user")
                    trySend(Resource.Success(user))
                } else {
                    Log.d(TAG, "fetchCurrentUser: user not found in $TAG")
                    trySend(Resource.Error("User data is null or malformed"))
                }
            }
        // Cancel the listener when the flow collector is closed
        awaitClose {
            Log.d(TAG, "fetchCurrentUser: listener removed in $TAG")
            listenerRegistration.remove()
        }
    }.flowOn(Dispatchers.IO)


    /**
     * below code all about the authentication
     * **/
    suspend fun forgetPassword(email: String): Resource<Unit> = withContext(Dispatchers.IO) {
        Log.d(TAG, "resetPassword: reset password for $email in $TAG")
        try {
            // First check if email exists in admin collection
            if (!checkEmailExistence(email)) {
                return@withContext Resource.Error("This email is not registered")
            }

            // If email exists, send reset email
            auth.sendPasswordResetEmail(email)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.d(TAG, "resetPassword: caught error in ${e.localizedMessage} in ${TAG}} ")
            Resource.Error(e.localizedMessage ?: "Failed to send reset email")
        }
    }

    suspend fun checkEmailExistence(email: String): Boolean = withContext(Dispatchers.IO) {
        Log.d(TAG, "checkEmailValidation: $email in $TAG")
        try {
            val ref =
                firestore.collection(Constant.USERS).whereEqualTo("email", email).get().await()

            !ref.isEmpty
        } catch (e: Exception) {
            Log.d(TAG, "checkEmailValidation: caught error ${e.message} in $TAG")
            false
        }
    }

    suspend fun signIn(email: String, password: String): Resource<FirebaseUser?> =
        withContext(Dispatchers.IO) {
            return@withContext try {
                val result = auth.signInWithEmailAndPassword(email, password).await()
                Resource.Success(result.user)
            } catch (e: Exception) {
                Log.d(TAG, "signInTheUser: failed to sign the user in repository")
                Resource.Error(e.localizedMessage ?: "Failed to sign in", null)
            }
        }


    suspend fun createUserUsingEmailAndPassword(
        email: String, password: String
    ): Resource<Boolean> = withContext(Dispatchers.IO) {
        return@withContext try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            Log.d(TAG, "createUserUsingEmailAndPassword: ${result.user}")
            Resource.Success(true)
        } catch (e: Exception) {
            Log.e(TAG, "createUserUsingEmailAndPassword: ${e.localizedMessage}")
            Resource.Error(e.localizedMessage ?: "Failed to create user")
        }
    }


    suspend fun storeUserInFirestore(user: User): Resource<Boolean> = withContext(Dispatchers.IO) {
        return@withContext try {
            if (user.uid.isNullOrBlank()) {
                Log.e(TAG, "storeUserInFirestore: User ID is null or blank")
                return@withContext Resource.Error("Invalid user ID")
            }
            val userRef = firestore.collection(Constant.USERS).document(user.uid)
            userRef.set(user).await()
            Resource.Success(true)
        } catch (e: Exception) {
            Log.e(TAG, "storeUserInFirestore: Failed to store user - ${e.message}")
            Resource.Error(e.localizedMessage ?: "Failed to store user data")
        }
    }

    fun signOut() {
        Log.d(TAG, "signOut: sign out the user in $TAG")
        auth.signOut()
    }

    //Fetching all the complaints
    fun fetchAllComplaintsRealtime(uid: String): Flow<Resource<List<Complaint>>> = callbackFlow {
        Log.d(TAG, "fetchAllComplaintsRealtime: $uid in $TAG for fetch complaints")
        val listener =
            firestore.collection(Constant.COMPLAINTS).whereEqualTo("userId", uid)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(Resource.Error(error.message ?: "Unknown error"))
                        return@addSnapshotListener
                    }

                    val complaints = snapshot?.documents?.mapNotNull {
                        it.toObject(Complaint::class.java)
                    } ?: emptyList()
                    Log.d(TAG, "fetchAllComplaintsRealtime: $complaints in $TAG")
                    trySend(Resource.Success(complaints))
                }

        awaitClose { listener.remove() }
    }.flowOn(Dispatchers.IO)

    fun fetchAllAnonymousComplaintsRealtime(userTempId: String): Flow<Resource<List<Complaint>>> =
        callbackFlow {
            Log.d(TAG, "fetchAllAnonymousComplaintsRealtime: $userTempId in $TAG")
            val listener =
                firestore.collection(Constant.ANONYMOUS_COMPLAINTS)
                    .whereEqualTo("userTempId", userTempId)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            trySend(Resource.Error(error.message ?: "Unknown error"))
                            return@addSnapshotListener
                        }

                        val complaints = snapshot?.documents?.mapNotNull {
                            it.toObject(Complaint::class.java)
                        } ?: emptyList()
                        Log.d(TAG, "fetchAllAnonymousComplaintsRealtime: $complaints in $TAG")

                        trySend(Resource.Success(complaints))
                    }
            awaitClose { listener.remove() }
        }.flowOn(Dispatchers.IO)


    suspend fun submitAnonymousComplaint(complaint: Complaint): String =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "submitComplaint: trying to submit complaint")

                val documentRef = firestore.collection(Constant.ANONYMOUS_COMPLAINTS)
                    .add(complaint)
                    .await() // suspend until success/failure

                val docId = documentRef.id

                // Update complaintId field in the same document
                documentRef.update("complaintId", docId).await()

                Log.d(TAG, "submitComplaint: success with ID $docId")
                return@withContext docId

            } catch (e: Exception) {
                Log.e(TAG, "submitComplaint: error ${e.message}")
                throw e
            }
        }

    suspend fun submitComplaint(complaint: Complaint): String = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "submitComplaint: Attempting to submit complaint...")

            // Add complaint to Firestore
            val documentRef = firestore.collection(Constant.COMPLAINTS)
                .add(complaint)
                .await() // suspends until completed

            val docId = documentRef.id

            // Update the same document with the docId as complaintId
            documentRef.update("complaintId", docId).await()

            Log.d(TAG, "submitComplaint: Successfully submitted with ID: $docId")
            return@withContext docId

        } catch (e: Exception) {
            Log.e(TAG, "submitComplaint: Failed to submit complaint - ${e.message}", e)
            throw e // Rethrow so ViewModel can handle it via try-catch
        }
    }

    //Drive related functions
    suspend fun uploadFileToDrive(
        filePath: String,
        complaintName: String,
        userName: String,
    ): Resource<String> = withContext(Dispatchers.IO) {
        Log.d(TAG, "uploadFileToDrive: uploading file in $TAG")

        val file = File(filePath)
        val mimeType =
            URLConnection.guessContentTypeFromName(filePath) ?: "application/octet-stream"

        // Determine target folder based on user type
        val targetFolderName =
            if (userName == "Anonymous") "AnonymousUserFiles" else "AuthenticateUserFiles"
        val targetFolderId = createFolderIfNotExists("root", targetFolderName)
            ?: return@withContext Resource.Error("Failed to create/find target folder")

        val fileName = "${complaintName}_${userName}_${file.name}_${System.currentTimeMillis()}"
        Log.d(TAG, "Uploading file: $fileName")

        val fileMetadata = com.google.api.services.drive.model.File().apply {
            name = fileName
            parents = listOf(targetFolderId) // Upload inside the target folder
        }

        val mediaContent = FileContent(mimeType, file)

        return@withContext try {
            val uploadedFile =
                driveService.files().create(fileMetadata, mediaContent).setFields("id").execute()

            val fileId = uploadedFile.id
            Log.d(TAG, "File uploaded successfully: $fileId")

            /*shareFileWithUser(fileId)
            shareFileWithUser(targetFolderId)*/

            val publicDownloadableFileLink = makeFilePublic(fileId)
            Log.d(TAG, "uploadFileToDrive: downloadableFileLink = $publicDownloadableFileLink")
            Resource.Success(publicDownloadableFileLink)
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading file: ${e.message}")
            Resource.Error("Upload failed: ${e.message}")
        }
    }

    private suspend fun shareFileWithUser(id: String) = withContext(Dispatchers.IO) {
        val permission = Permission().apply {
            type = "user" // 'user' means a specific email
            role = "reader"
            emailAddress = Constant.SHARED_EMAIL_ID
        }

        try {
            driveService.permissions().create(id, permission).setSendNotificationEmail(true)
                .execute()
            Log.d(TAG, "File shared successfully with ${Constant.SHARED_EMAIL_ID}")
        } catch (e: Exception) {
            Log.e(TAG, "Error sharing file: ${e.message}")
        }
    }

    private fun makeFilePublic(fileId: String): String {
        val permission = Permission().apply {
            type = "anyone"
            role = "reader"
        }

        return try {
            driveService.permissions().create(fileId, permission).execute()
            val downloadableFileLink = "https://drive.google.com/file/d/$fileId/view"
            Log.d(TAG, "File is now public: $downloadableFileLink")
            downloadableFileLink
        } catch (e: Exception) {
            Log.e(TAG, "Error making file public: ${e.message}")
            "${e.message}"
        }
    }

    // Function to check if a folder exists, or create a new one
    private fun createFolderIfNotExists(parentId: String?, folderName: String): String? {
        return try {
            val query =
                "name='$folderName' and mimeType='application/vnd.google-apps.folder' and trashed=false" +
                        (parentId?.let { " and '$it' in parents" } ?: "")

            val existingFolders =
                driveService.files().list().setQ(query).setFields("files(id, name)").execute().files

            if (existingFolders.isNotEmpty()) {
                return existingFolders.first().id // Folder already exists, return its ID
            }

            // If folder does not exist, create it
            val folderMetadata = com.google.api.services.drive.model.File().apply {
                name = folderName
                mimeType = "application/vnd.google-apps.folder"
                parentId?.let { parents = listOf(it) }
            }

            val createdFolder =
                driveService.files().create(folderMetadata).setFields("id").execute()

            Log.d(TAG, "Folder created: ${createdFolder.id}")
            createdFolder.id
        } catch (e: Exception) {
            Log.e(TAG, "Error creating folder: ${e.message}")
            null
        }
    }

    suspend fun deleteFile(fileId: String): Resource<Boolean> = withContext(Dispatchers.IO) {
        return@withContext try {
            driveService.files().delete(fileId).execute()
            Log.d(TAG, "File deleted successfully: $fileId")
            Resource.Success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting file: ${e.message}")
            Resource.Error("Failed to delete file: ${e.message}")
        }

    }

    suspend fun sendComplaintEmail(complaint: Complaint) = withContext(Dispatchers.IO) {
        Log.d(TAG, "sendComplaintEmail: Starting email send process")
        val senderPassword = PrivateData.SENDER_MAIL_PASSWORD
        val senderMail = PrivateData.SENDER_MAIL
        val receiverMail = PrivateData.RECEIVER_COMPLAINT_MAIL
        try {
            Log.d(TAG, "sendComplaintEmail: preparing to send email")

            val email = GMailSender(senderMail, senderPassword)

            val sdf = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault())
            val uploadDateAndTime = sdf.format(complaint.timestamp.toDate())

            val body = """
        New Complaint Submitted

        Complaint Subject: ${complaint.complaintTitle}
        Complaint Description: ${complaint.complaintDescription}
        User is Anonymous: ${complaint.isAnonymous}
        District: ${complaint.district}
        Tehsil: ${complaint.tehsil}
        Village: ${complaint.village}
        Name: ${complaint.personName}
        Phone Number: ${complaint.phoneNumber}
        Device Id: ${complaint.deviceId}
        IP address: ${complaint.ipAddress}
        Upload Date & Time: $uploadDateAndTime

        Files:
        ${complaint.fileUrl}
    """.trimIndent()

            Log.d(TAG, "sendComplaintEmail: Attempting to send email to $receiverMail")
            email.sendMail(
                subject = "New Complaint: ${complaint.complaintTitle}",
                body = body,
                recipient = receiverMail
            )

            Log.d(TAG, "sendComplaintEmail: Email sent successfully")

        } catch (e: Exception) {
            Log.e(TAG, "sendComplaintEmail: Failed to send email", e)
            Log.e(TAG, "sendComplaintEmail: Error details - ${e.message}")
            Log.e(TAG, "sendComplaintEmail: Stack trace - ${e.stackTraceToString()}")
            throw e // Rethrow to handle in ViewModel
        }
    }

    suspend fun sendJoinMemberEmail(user: User) = withContext(Dispatchers.IO) {
        Log.d(TAG, "sendJoinMemberEmail: Starting email send process")
        val senderPassword = PrivateData.SENDER_MAIL_PASSWORD
        val senderMail = PrivateData.SENDER_MAIL
        val receiverMail = PrivateData.RECEIVER_USER_MAIL
        try {
            Log.d(TAG, "sendJoinMemberEmail: preparing to send email")

            val email = GMailSender(senderMail, senderPassword)

            val sdf = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault())
            val uploadDateAndTime = sdf.format(user.timestamp.toDate())

            val body = """
        New Member Join Us

        Member Name: ${user.name}
        Member Phone Number: ${user.phoneNumber}
        Member Email ID: ${user.email}
        Member District: ${user.district}
        Member Tehsil: ${user.tehsil}
        Member Village: ${user.village}
        Member Authenticate: ${user.isAuthenticate}
        Member Device Id: ${ShardPref.getDeviceId(Constant.DEVICE_ID)}
        Member IP Address: ${ShardPref.getIpId(Constant.IP_ADDRESS)}
        Member joining Date & Time: $uploadDateAndTime

    """.trimIndent()

            Log.d(TAG, "sendJoinMemberEmail: Attempting to send email to $receiverMail")
            try {
                email.sendMail(
                    subject = "New Member: ${user.name}",
                    body = body,
                    recipient = receiverMail
                )
                Log.d(TAG, "sendJoinMemberEmail: Email sent successfully")
            } catch (e: Exception) {
                Log.e(TAG, "sendJoinMemberEmail: Failed to send email", e)
                Log.e(TAG, "sendJoinMemberEmail: Error details - ${e.message}")
                Log.e(TAG, "sendJoinMemberEmail: Stack trace - ${e.stackTraceToString()}")
                throw e
            }

        } catch (e: Exception) {
            Log.e(TAG, "sendJoinMemberEmail: Failed to prepare email", e)
            Log.e(TAG, "sendJoinMemberEmail: Error details - ${e.message}")
            Log.e(TAG, "sendJoinMemberEmail: Stack trace - ${e.stackTraceToString()}")
            throw e
        }
    }

    suspend fun uploadComplaintAndSetId(complaint: Complaint): Boolean =
        withContext(Dispatchers.IO) {
            Log.d(TAG, "uploadComplaintAndSetId: ")
            return@withContext try {
                Log.d(TAG, "uploadComplaintAndSetId: Attempting to submit complaint...")

                // Add complaint to Firestore
                val documentRef = firestore.collection(Constant.COMPLAINTS)
                    .add(complaint)
                    .await() // suspends until completed

                val docId = documentRef.id

                // Update the same document with the docId as complaintId
                documentRef.update("complaintId", docId).await()
                documentRef.update("isAnonymous", false).await()

                Log.d(TAG, "uploadComplaintAndSetId: Successfully submitted with ID: $docId")
                return@withContext true

            } catch (e: Exception) {
                Log.e(TAG, "uploadComplaintAndSetId: Failed to submit complaint - ${e.message}", e)
                throw e // Rethrow so ViewModel can handle it via try-catch
            }
        }

    companion object {
        private const val TAG = "Repository"
    }
}