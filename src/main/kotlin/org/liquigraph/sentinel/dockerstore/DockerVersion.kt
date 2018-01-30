package org.liquigraph.sentinel.dockerstore

import com.google.gson.annotations.SerializedName

data class DockerVersion(@SerializedName("full_description") var fullDescription: String)