# Overview
This is a toy project that creates a web service
which gives the most popular game mode played in a region
You can use true IP address in the request or fake coutry
codes in the request for simplifying tests purpose

# System requirement
## Eclipse indigo or +
## Google App Engine Plugin
## Java JRE 1.7

# How to run the toy project
Checkout both MostPopularWS and MostPopularWSClient
Open eclipse and import both of the projects. Run
MostPopularWS as a web application. When it is up and running.
Run MostPopularWSClient as a java application. The client
will test the functionality of the service

#Design
##
Client post a GET request to the service
##
Service gets the client game mode from the cookie it sends
##
If you are working with real IP address, which would be the case in production
mode, the service will get the country code from Remote_Addr directly which is
more reliable. If you are just testing the service, you can send country code
directly in the service request. It is stored in cookie as well
## The count of each game mode per country is stored in the data store while 
the keys for the data store is cached in memcache
## I am using cookies instead of REST API cause I don't want to make it a public API 

# Scalabilty
The service can be scaled up by deploying multiple instances behind a load balancer to serve millions of gamers at one time
