package com.example.commerce.product

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.Date

@Service
class ProductService(private val productClient: ProductClient,
        private val redisTemplate: RedisTemplate<String, String>) {

    //Object <-> JSON간의 변환 객체와 제이슨간의 변환
    private val mapper = jacksonObjectMapper()

    val topProducts = listOf<TopProductResponse>()

    //애플리케이션을 시작하면 한번은 수행됨
    @Scheduled(fixedRate = 1000 * 60 * 60)
    fun scheduledFetchTopPromotion() {
        println("--called by schedule: ${Date().time}--")
//        val topProducts = productClient.getTopPromotion();
        //위처럼 해도 되지만 재시작할 때마다 내용이 리셋되기 때문에 아래처럼 redis를 쓰는 것..
        //레디스도 완전히 서버를 껐다 켜면 다 지워지긴 함.

        val result = productClient.getTopPromotion();
        println(result)

        //RedisTemplate<key=String, value=String>
        //default: localhost:6379
        redisTemplate.delete("top-promotion") //캐시 데이터 삭제
        //캐시 데이터 생성
        redisTemplate.opsForValue()
                .set("top-promotion", mapper.writeValueAsString(result))
    }

    fun getCachedTopPromotion() : List<TopProductResponse> {
        val result = redisTemplate.opsForValue().get("top-promotion")
        if(result != null) {
            //value(json) -> List<TopProductResponse> 객체변환
//            val list : List<TopProductResponse> = mapper.readValue(result)
//            return list 위 두줄을 아래의 하나로 줄인다.
            return mapper.readValue(result)
        } else {
            //빈배열 반환
            return listOf()
        }
    }
}