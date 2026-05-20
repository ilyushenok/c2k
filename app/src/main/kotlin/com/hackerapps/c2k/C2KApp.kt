package com.hackerapps.c2k

import android.app.Application
import com.hackerapps.c2k.data.db.AppDatabase
import com.hackerapps.c2k.data.repository.SessionRepository

class C2KApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.create(this) }
    val sessionRepository: SessionRepository by lazy { SessionRepository(database) }

}
