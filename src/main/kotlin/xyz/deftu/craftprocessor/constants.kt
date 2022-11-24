package xyz.deftu.craftprocessor

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import org.apache.logging.log4j.LogManager

const val NAME = "@PROJECT_NAME@"
const val VERSION = "@PROJECT_VERSION@"
const val GIT_BRANCH = "@GIT_BRANCH@"
const val GIT_COMMIT = "@GIT_COMMIT@"
const val COLOR = 0xC91212
val LOGGER = LogManager.getLogger(NAME)
val GSON = GsonBuilder()
    .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
    .setPrettyPrinting()
    .setLenient()
    .create()
