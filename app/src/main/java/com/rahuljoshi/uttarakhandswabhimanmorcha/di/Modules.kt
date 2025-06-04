package com.rahuljoshi.uttarakhandswabhimanmorcha.di

import android.content.Context
import androidx.room.Room
import com.google.api.client.http.HttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.rahuljoshi.uttarakhandswabhimanmorcha.model.local.ComplaintDao
import com.rahuljoshi.uttarakhandswabhimanmorcha.model.local.ComplaintDatabase
import com.rahuljoshi.uttarakhandswabhimanmorcha.model.repository.ComplaintLocalRepository
import com.rahuljoshi.uttarakhandswabhimanmorcha.model.repository.Repository
import com.rahuljoshi.uttarakhandswabhimanmorcha.model.repository.UpdateRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class Modules {

    @Provides
    fun provideDatabase(@ApplicationContext context: Context): ComplaintDatabase =
        Room.databaseBuilder(context, ComplaintDatabase::class.java, "complaint_db").build()

    @Provides
    fun provideDao(db: ComplaintDatabase): ComplaintDao = db.complaintDao()

    @Provides
    @Singleton
    fun provideComplaintLocalRepository(
        complaintDao : ComplaintDao
    ) : ComplaintLocalRepository{
        return ComplaintLocalRepository(complaintDao)
    }

    @Provides
    @Singleton
    fun provideUpdateRepository(
        firestore: FirebaseFirestore
    ) : UpdateRepository{
        return UpdateRepository(firestore)
    }

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    @Provides
    @Singleton
    fun provideRepository(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth,
        driveService: Drive
    ): Repository {
        return Repository(firestore, auth, driveService)
    }


    @Provides
    @Singleton
    fun provideHttpTransport(): HttpTransport {
        return NetHttpTransport()
    }

    @Provides
    @Singleton
    fun provideJsonFactory(): JsonFactory {
        return GsonFactory.getDefaultInstance()
    }

    @Provides
    @Singleton
    fun provideGoogleCredentials(@ApplicationContext context: Context): GoogleCredentials {
        val assetManager = context.assets
        val inputStream = assetManager.open("service_account.json")
        return GoogleCredentials.fromStream(inputStream)
            .createScoped(listOf(DriveScopes.DRIVE_FILE, DriveScopes.DRIVE))
    }

    @Provides
    @Singleton
    fun provideDriveService(
        httpTransport: HttpTransport,
        jsonFactory: JsonFactory,
        googleCredential: GoogleCredentials
    ): Drive {
        return Drive.Builder(httpTransport, jsonFactory, HttpCredentialsAdapter(googleCredential))
            .setApplicationName("Uttarakhand Swabhiman Morcha")
            .build()
    }
}