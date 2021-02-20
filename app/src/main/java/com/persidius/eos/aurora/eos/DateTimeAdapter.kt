package com.persidius.eos.aurora.eos

import com.apollographql.apollo.api.CustomTypeAdapter
import com.apollographql.apollo.api.CustomTypeValue

object DateTimeAdapter : CustomTypeAdapter<String> {
    override fun decode(value: CustomTypeValue<*>): String {
        return value.value.toString()
    }

    override fun encode(value: String): CustomTypeValue<*> {
        return CustomTypeValue.GraphQLString(value)
    }
}