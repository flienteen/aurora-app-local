package com.persidius.eos.aurora.util

data class Tuple2<T1, T2>(val first: T1, val second: T2)

data class Tuple3<T1, T2, T3>(val first: T1, val second: T2, val third: T3)

data class Tuple4<T1, T2, T3, T4>(val first: T1, val second: T2, val third: T3, val fourth: T4)

data class Tuple5<T1, T2, T3, T4, T5>(val first: T1, val second: T2, val third: T3, val fourth: T4, val fifth: T5)

data class Tuple6<T1, T2, T3, T4, T5, T6>(val first: T1, val second: T2, val third: T3, val fourth: T4, val fifth: T5, val sixth: T6)

data class Tuple7<T1, T2, T3, T4, T5, T6, T7>(val first: T1, val second: T2, val third: T3, val fourth: T4, val fifth: T5, val sixth: T6, val seventh: T7)

infix fun <T1, T2> T1.then(second: T2) = Tuple2(this, second)
infix fun <T1, T2, T3> Tuple2<T1, T2>.then(third: T3) = Tuple3(this.first, this.second, third)
infix fun <T1, T2, T3, T4> Tuple3<T1, T2, T3>.then(fourth: T4) = Tuple4(this.first, this.second, this.third, fourth)
infix fun <T1, T2, T3, T4, T5> Tuple4<T1, T2, T3, T4>.then(fifth: T5) = Tuple5(this.first, this.second, this.third, this.fourth, fifth)
infix fun <T1, T2, T3, T4, T5, T6> Tuple5 <T1, T2, T3, T4, T5>.then(sixth: T6) = Tuple6(this.first, this.second, this.third, this.fourth, this.fifth, sixth)
infix fun <T1, T2, T3, T4, T5, T6, T7> Tuple6<T1, T2, T3, T4, T5, T6>.then(seventh: T7) = Tuple7(this.first, this.second, this.third, this.fourth, this.fifth, this.sixth, seventh)