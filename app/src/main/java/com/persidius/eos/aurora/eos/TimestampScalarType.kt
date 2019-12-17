package com.persidius.eos.aurora.eos

import com.apollographql.apollo.response.CustomTypeAdapter
import com.apollographql.apollo.response.CustomTypeValue

object TimestampScalarType: CustomTypeAdapter<Long> {
    override fun decode(value: CustomTypeValue<*>): Long {
        return value.value.toString().toLong(10)
    }

    override fun encode(value: Long): CustomTypeValue<*> {
        return CustomTypeValue.GraphQLNumber(value)
    }
}
