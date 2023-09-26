package com.example.commerce

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable

@SpringBootApplication
@EnableFeignClients
@EnableScheduling
class CommerceApplication

fun main(args: Array<String>) {
	runApplication<CommerceApplication>(*args)
}

//다른 서비스(서버)에 HTTP 요청을 보내는 클라이언트를 작성
//192.168 : 사설 IP 대역
@FeignClient(name = "inventoryClient", url = "http://192.168.100.36:8082/inventories")
interface InventoryClient {
	@GetMapping("/{productId}")
	fun fetchProductStocks(@PathVariable productId: Int): Int?



}

