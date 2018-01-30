package org.liquigraph.sentinel.github

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter

class SemanticVersionAdapter: TypeAdapter<SemanticVersion>() {

    override fun write(out: JsonWriter?, value: SemanticVersion?) = TODO()
    override fun read(reader: JsonReader): SemanticVersion {
        if (reader.peek() === JsonToken.NULL) {
            reader.nextNull()
            throw IllegalArgumentException("Null not supported for versions")
        }
        val raw = reader.nextString()
        return SemanticVersion.parseEntire(raw)!!
    }
}