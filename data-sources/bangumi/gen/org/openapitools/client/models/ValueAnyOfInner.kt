/**
 *
 * Please note:
 * This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * Do not edit this file manually.
 *
 */

@file:Suppress(
    "ArrayInDataClass",
    "EnumEntryName",
    "RemoveRedundantQualifierName",
    "UnusedImport"
)

package org.openapitools.client.models

import org.openapitools.client.models.KV
import org.openapitools.client.models.V

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * 
 *
 * @param k 
 * @param v 
 */


data class ValueAnyOfInner (

    @Json(name = "k")
    val k: kotlin.String,

    @Json(name = "v")
    val v: kotlin.String

)
