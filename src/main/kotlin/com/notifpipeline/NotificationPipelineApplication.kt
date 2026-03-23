package com.notifpipeline

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class NotificationPipelineApplication

fun main(args: Array<String>) {
	runApplication<NotificationPipelineApplication>(*args)
}
