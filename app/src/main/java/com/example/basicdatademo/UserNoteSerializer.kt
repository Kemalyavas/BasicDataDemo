package com.example.basicdatademo

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream

// Proto DataStore'un UserNote'u dosyaya nasıl yazıp okuyacağını belirler
object UserNoteSerializer : Serializer<UserNote> {

    // İlk açılışta dosya yoksa kullanılacak boş değer
    override val defaultValue: UserNote = UserNote.getDefaultInstance()

    // Dosyadan oku — bozuksa CorruptionException fırlat
    override suspend fun readFrom(input: InputStream): UserNote {
        try {
            return UserNote.parseFrom(input)
        } catch (e: InvalidProtocolBufferException) {
            throw CorruptionException("UserNote okunamadı", e)
        }
    }

    // Dosyaya yaz
    override suspend fun writeTo(t: UserNote, output: OutputStream) {
        t.writeTo(output)
    }
}
