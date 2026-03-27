package com.notifpipeline.messaging

interface BrokerDestinationResolver {
    fun resolve(destination: BrokerDestination): String
}
