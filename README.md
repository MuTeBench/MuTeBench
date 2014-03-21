/*******************************************************************************
 * 
 *  MuTeBench - Benchmark for multi-tenant Database Systems
 * 
 *  Creators:    Andreas Goebel <andreas.goebel@uni-jena.de>
 *				Contributors of OLTP-BENCH (see oltpbenchmark-CONTRIBUTORS.md)
 * 
 *  This library is free software; you can redistribute it and/or modify it 
 *  under the terms of the GNU General Public License as published by the Free 
 *  Software Foundation; either version 3.0 of the License, or (at your option) 
 *  any later version.
 * 
 *  This library is distributed in the hope that it will be useful, but WITHOUT 
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or 
 *  FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License 
 *  for more details.
 ******************************************************************************/

 
                            MUTEBENCH 0.6 - README
                            ======================
 
	
/*******************************************************************************
 *******************************************************************************
 * 1. Overview and Concepts
 *******************************************************************************
 *******************************************************************************/

	MuTeBench is an open-source framework for creating multi-tenant DBS benchmarks. 
	We have implemented it with the purpose of being able to run scalability tests 
	and performance isolation tests for a variety of multi-tenant DBMSs and DBaaS 
	offerings. It provides flexible scheduling of various tenant workloads,
	modelling of evolving usage patterns and fine-grained statistic gathering.

	Instead of developing a new framework, we decided to extent OLTP-Bench
	(http://oltpbenchmark.com/), which is as an ideal starting point for a 
	MT-DBMS benchmark framework. 
	
	
/*******************************************************************************
 * 1.1 Benchmarking multi-tenant DBMSs
 *******************************************************************************/
	
	MuTeBench's flexible scenario definitions and evolving workload rates / mixes 
	allow the construction of diverse workloads. This can be used for testing 
	the major challenges of multi-tenant DBMS like scalability, performance 
	predictability and tenant performance isolation.
	
	A multi-tenant DBMS may isolate tenants in a shared database system 
	  - by dedicated databases (shared machine approach), 
	  - by shared databases and separate tables or schemas (shared process 
	    approach) or 
	  - by an association of each dataset in a shared table with the appropriate 
	  	tenant (shared table approach).
	
	From a application's perspective, multi-tenancy has to be transparently. A 
	multi-tenant DBMS maps an incoming database connection to the appropriate 
	tenant and only use either its database (shared machine), or its schemas/tables 
	(shared process) or just its datasets within tables (shared table) for 
	transaction processing. However, by setting tenant-specific connection 
	settings MuTeBench also supports DBMSs which not hide the location of tenant 
	data : 
	  -	You can use tenant specific configuration files, each containing the 
	  	database address and access data of the appropriate tenant.
	  -	You can use a single configuration file for all tenants containing 
	  	tenant specific database access data and database addresses (using 
	  	different databases for shared machine and different default schemas 
	  	for shared process).
	
	MuTeBench is not able to create tables which are shared by several tenants
	and associate each row with its owner by an association column. Shared
	table is supported only for DBMS which undertake tenant association (e. g. 
	according to connection settings) and query transformation.  
	
	
/*******************************************************************************
 * 1.2 Statistic Gathering and Baseline Runs
 *******************************************************************************/

	After an completed benchmark scenario MuTeBench aggregates several performance 
	results for a given sampling window (see parameter '-a' in Section 3.1): 
	  -	average throughput, minimum latency, average latency, maximum latency, 
	  -	relevant latency percentiles (25th, 50th, 75th, 90th, 95th, and 99th percentile) 
	  -	and maybe the penalty amount for a given Service Level Agreement (SLA). 
	We call them absolute performance metrics. 
	
	For measuring isolation of tenant performance, the impact of other active 
	tenants on the transaction processing performance of a given tenant has to be 
	determined. Therefore we calculate relative tenant performance (rate of tenant 
	performance with respect to its best possible performance amount in a single 
	tenant environment). For its calculation, the best possible tenant performance 
	in a simulated single-tenant environment has to be determined first. For this 
	purpose tenants may run workloads within an initial baseline run without any
	resource competitors. Following this, during actual multi-tenant runs 
	latencies and throughput of transactions (or single queries) will be gathered. 
	They are set in proportion to their baseline equivalents relatively, resulting 
	in relative performance values:
	
	relative_throughput 	= test_throughput / baseline_throughput  
	relative_latency_metric = 1 / (test_latency_metric / baseline_latency_metric)  
	  							  
	Please note: 
	  -	Relative tenant performance takes into account time delays of each tenant 
	  	in its baseline run and the following test run. 
	  	Example: 
	  		Baseline run: Workload of tenant A is active from 5 minutes until 15 minutes
	  		Test run: Workload of tenant A from 15 until 25 minutes
	  		-> MuTeBench will compare the compatible periods of time. 
		This allows you to use a single baseline run for several tenants in a shared 
		test run, for example.
		
	  -	Absolute and relative performance metrics are calculated using all
	  	latencies of the executed benchmark, whereas Service Level Agreements may
	  	be limited to execution performance results of certain transaction types.
	
/*******************************************************************************
 * 1.3 Wildcards
 *******************************************************************************/

	For some parameters defined in the Scenario Description File (Section 3.1) and 
	SLA Definition File (Section 3.2) wildcards can be used to allow individual 
	parameter values for each tenant. MuTeBench only supports the wildcard '# tid',
	which is replaced by the appropriate tenant ID. This substitution is enabled by 
	surrounding the parameter value with '@'.  
	
	Example (using tenant 5): 
	<username>@tenant#tid@</username>   -->   <username>tenant5</username> 
	
 
/*******************************************************************************
 *******************************************************************************
 * 2. Installation
 *******************************************************************************
 *******************************************************************************/

	1. 	Checkout the source code from the svn repository (it's coming soon!)
		svn checkout http://?.googlecode.com/svn/trunk/ mutebench

	2. 	Afterwords, you have to compile MuTeBench, for example with the help of 
		our Apache Ant script (./mutebench_build.xml) to automatically build the 
		system.

	3. 	Prepare the database: If you start from scratch, MuTeBench is able to 
		create required tables (see action 'create' in Section 3.2) or generate 
		required data (see action 'load' in Section 3.2). However it 
		expects an existing database (or even more than one for separate 
		tenant databases) and also existing schemas if desired. You have to 
		provide the database login credentials to MuTeBench within the 
		configuration file (see Section 3.4). Several configuration file examples
		are provided in the directory './config' like 
		'./config/mutebench_config_sample'.

	4.	Finally you have to start MuTeBench, it's parameters are described in 
		Section 3.1. We also provide a script './mutebench_runscript' and an 
		example how to run it ('./mutebench_runscript_sample'), which can be 
		used for customized scripts.


/*******************************************************************************
 *******************************************************************************
 * 3. Configuration
 *******************************************************************************
 *******************************************************************************/

	MuTeBench uses different files and provides some parameters to adjust its 
	behavior to your needs.


/*******************************************************************************
 * 3.1 MuTeBench parameters
 *******************************************************************************/
 
	Main class: com.oltpbenchmark.multitenancy.MuTeBench
	
	VM arguments: 
	
	>	'Dlog4j.configuration' [file]: path and name of property file
			example: -Dlog4j.configuration=log4j.properties
	
	Program arguments:

	>	'-a' | '--analysis-buckets': sampling buckets for result aggregation
			(optional parameter, format: [seconds], default: no sampling)
			example: -a 10

	>	'-b' | '--baseline': Output file name of a previous baseline run
			(optional parameter, format: [file], default: no baseline file)
			example: -b results/run1
								
	>	'-d' | '--dialects-export': Export benchmark SQL to a dialects file
			(optional parameter, format: [file], default: no export)
			example: -d export/run1			

	>	'-g' | '--gui': display GUI to analyze results
			(optional parameter, default: no GUI)
			example: -g

	>	'-h' | '--help': print parameter overview and description
			(optional parameter, default: no help)
			example: -h 
			
	>	'--histograms': Print histograms with statistics for each TA Type
			(optional parameter, default: no histograms)
			example: -h
				
	>	'-o' | '--output': path and name of the MuTeBench Result File 
			(optional parameter, format: [file], default: no output)
			example: -o results/run1
				
	>	'-s' | '--scenario': Path and name of the MuTeBench Scenario 
			Description File 
			(required parameter, format: [file])
			example: -s config/sample_mutebench_scenario.xml
 
	>	'-r' | '--runtime': Maximum runtime of MuTeBench
			(optional parameter, format: [hh:mm:ss], default: run until 
			scenario ends)
			example: -r 01:00:00
				
	>	'-v' | '--verbose': display messages
			(optional parameter, default: no messages)
			example: -v			
		 	
		
/*******************************************************************************
 * 3.2 Editing the MuTeBench Scenario Description File
 *******************************************************************************/
			
 	The scenario file includes a lot of events for controlling the behavior of 
 	MuTeBench. Each event is associated with an workload run and can be executed 
 	by multiple tenants at different times. It is defined as a sub node 'event' 
 	under a XML node 'events' and contains the following parameters as sub nodes:
 		
 		
 	Execution time parameters:
 	
 	>	'start' [hh:mm:ss]: starting time
 			description: Time of first benchmark execution (MuTeBench will wait
 				this time interval after its start)
 				(optional parameter, default: immediate start)
			example: <start>00:05:00</start>
 		
 	>	'repeat' [hh:mm:ss]: interval during event executions
 			description: With the help of this parameter you can execute 
 				the corresponding event frequently. The value defines the 
 				interval between each execution time.
 				(optional parameter, default: only one benchmark execution time)
			example: <repeat>00:05:00</repeat>
 
  	>	'stopAfter' [hh:mm:ss]: last execution time
 			description: In conjunction with 'repeat' this parameter defines
 				the time of the last event execution
				(optional parameter, default: only one benchmark execution time)
			example: <end>00:15:00</end>
 	
 	
 	Tenant association parameters:
 		 	
 	>	'tenantsPerExecution' [integer]: number of benchmark executions for 
 				each execution time
    		description: For each execution time a benchmark run may be 
    			started for several tenants. This parameter sets the number 
    			of benchmark runs.
    			(optional parameter, default: 1)
			example: <tenantsPerExecution>3</tenantsPerExecution>
    	
    >	'firstTenantID' [int]: Tenant ID for first benchmark run
    		description: Defines the ID for the tenant which will be 
    			associated with the first benchmark run. 
				(optional parameter, default: 1)
			example: <firstTenantID>3</firstTenantID>	
    		
    >	'tenantIdIncrement' [int]: Increment for associating tenant IDs for 
    			the second and further benchmark runs
    		description: Defines the increment for associating tenant IDs 
    			with the second and further benchmark runs. 
				(optional parameter, default: 1)
			example: <tenantIdIncrement>3</tenantIdIncrement>	
    	
    		
    Benchmark configuration:		
    		
 	>	'benchmark' [string]: benchmark name
			description: Supported name of the benchmark which will be 
				executed by each benchmark run. This parameter supports 
				wildcards allowing individual benchmarks for each tenant. 
				However, this is restricted with respect to the benchmark 
				name.
			example: <benchmark>tpcc</benchmark>
			
	>   'configFile' [file]: configuration file for each benchmark run
    		description: Path and name of the configuration file used 
    			for each benchmark run. The structure of this file is 
    			described in Section 3.4. This parameter supports wildcards 
    			allowing the use of individual configuration files for each 
    			tenant.
			example: <configFile>config/run1.xml</configFile>
			
	>  	'actions' [string]: list of actions to be executed by each benchmark 
	   			run
    		description: Each benchmark run may execute one or more of the 
    			following actions that correspond to program arguments of a 
    			OLTP-Bench run:
    			- create: 		Initialize the database for this benchmark
    							by creating needed tables
    			- load: 		Load data using the benchmark's data loader
    			- execute:		Execute the benchmark workload
    			- runscript: 	Run an SQL script
    			- clear:		Clear all records in the database for this 
    							benchmark
    			See also http://oltpbenchmark.com/wiki/index.php?title=Quickstart#Target_benchmarks
    			The parameter is defined as a comma separated list of actions:
			example: <actions>create,load,execute</actions>			
		
			
	Service Level Agreements:		
			
	>	'benchmarkSlaFile' [file]: File with Service Level Agreements 
			description: Path and file name of the SLA penalty description  
				file, its structure is described in Section 3.3. This parameter 
				supports wildcards allowing the use of individual sla files 
				for each tenant.
 				(optional parameter, default: no SLAs)
			example: 
				<benchmarkSlaFile>sample/SLA-standard.xml</benchmarkSlaFile>
 
 	>	'sendSla' [boolean]: Forward SLAs to DBMS
 			description: Tenant SLAs can be forwarded to the DBMS using 
 				a DBMS with support of proprietary SQL statements by Andreas 
 				Goebel.
 				(optional parameter, default: no forwarding)
			example:
				<sendSLA>true</sendSLA>

    		
	Example of a scenario description file, which defines 6 workload 
		executions (0 min.: Tenant ID 1 and 4; after 5 min.: ID 7 and 10; 
		after 10 min.: 13 and 16) and clear the used data of these
		tenants after 30 minutes: (filename: 
		./config/scenarios/mutebench_scenario_sample)
	
	<?xml version="1.0"?>
	<parameters>
		<events>
			<event>														   
				<!--the event will be executed first after 1 minute-->
				<start>00:01:00</start> 								
				
				<!--repeat event execution after 5 minutes-->
				<repeat>00:05:00</repeat>								
				
				<!--stop repeating after 13 minutes-->
				<stopAfter>00:13:00</stopAfter>							
				
				<!--the first execution is associated to tenant ID 1-->
				<firstTenantID>1</firstTenantID>						
				
				<!--create two tenants for each execution time-->
				<tenantsPerExecution>2</tenantsPerExecution>			
				
				<!--increment used tenant ID by 3-->
				<tenantIdIncrement>3</tenantIdIncrement>				
				
				<!--only execute workloads for each benchmark run-->
				<actions>execute</actions>								
				
				<!--start each benchmark with configuration file 
				mutebench_config_sample.xml -->
				<configFile>mutebench_config_sample.xml</configFile>				
				
				<!--set SLAs in sla/standard.xml for each benchmark 
				run and each tenant-->
				<benchmarkSlaFile>sla/standard.xml</benchmarkSlaFile>	
				
				<!--send SLAs to a database with support of our 
				SQL extensions-->
				<sendSla>true</sendSla>									
				
				<!--benchmark to be executed-->
				<benchmark>tpcc</benchmark>	
			</event>
	        <event>
	          	<start>00:30:00</start>
	          	<firstTenantID>1</firstTenantID>
	          	<tenantsPerExecution>6</tenantsPerExecution>
	          	<tenantIdIncrement>3</tenantIdIncrement>
	          	<actions>clear</actions>
	          	<configFile>config/run1.xml</configFile>
				<benchmark>tpcc</benchmark>							
	          </removeTenant>
	    	</event>
		</events>


/*******************************************************************************
 * 3.3  Instructions for editing the Benchmark SLA definition file
 *******************************************************************************/
 
 	The SLA definition file sets tenant aware service level agreements. They
 	are defined for all benchmark runs of an associated tenant until new SLAs 
 	will be defined for its future benchmark runs. The parameters are defined 
 	as sub nodes under a xml node 'parameter':
 	
 	>	'benchmark' - name of the benchmark, it has to be the same name like in
 			'benchmark' in your scenario event description 

	>	'window' - time slot for measurement

	>	'sla' - SLA Definition
			definition: Each 'sla' node defines a SLA. You can define more than
			one definition in this file using several 'sla' nodes. The 
		
	
	
	Each SLA definition contains the following parameters as sub nodes:
	 		
 	>	'name' - name of the SLA. Each SLA name and its according sla definition
 			will be stored only once in the database to avoid storage overhead. 
 			Make sure that you never give the same name to two different SLA
 			definitions

	>	'transactions' - list of transactions which are affected by this SLA
			Value 'ALL' defines that all current and future transactions are 
			affected With the help of one or more '<ta>taNr</ta>' you can define 
			an affected transaction, taNr stands for the transaction number in 
			your benchmark configuration

 	>	'metric' - kind of service level agreement. The following metrics are 
 			currently supported:
 			'THROUGHPUT': determines aims regarding throughput [TXs/seconds]
			'LATENCY_AVERAGE': determines aims regarding average execution time 
				of transactions [milliseconds]
			'LATENCY_MINIMUM': determines aims regarding minimal execution time 
				[milliseconds]
			'LATENCY_MAXIMUM': determines aims regarding maximal execution time 
				[milliseconds]
			'LATENCY_25TH_PERCENTILE': 25th Latency Percentile [milliseconds]
			'LATENCY_MEDIAN': 50th Latency Percentile [milliseconds]
			'LATENCY_75TH_PERCENTILE': 75th Latency Percentile [milliseconds]
			'LATENCY_90TH_PERCENTILE': 90th Latency Percentile [milliseconds]
			'LATENCY_95TH_PERCENTILE': 95th Latency Percentile [milliseconds]
			'LATENCY_99TH_PERCENTILE': 99th Latency Percentile [milliseconds]
					
	>	'scope' - kind of reference for metric values, three scopes are 
			currently supported and defined as content of the sub node 'name':
			'single': every sla violation is followed by a penalty
			'average' (for latency only): only a sla violation of the average 
				latency of a transaction class is followed by a penalty
			'rate' (for latency only): only a sla violation of a chosen rate 
				of all transactions in a transaction class is followed by a 
				penalty
			Suppose that you choose the scope 'rate', you have to define that 
			rate in another sub node 'amount', which value have to be between
			0.0 and 1.0
			
	>	'serviceLevels' - you can define several service levels, which are 
			defined referred to a sub node 'sl'. Each service level has some 
			sub nodes by itself: 'metricAmount' - aims regarding execution 
			time [milliseconds] or throughput [TXs/seconds] 'penalty' - if the 
			measured execution time or throughput does not agree on the 
			metricAmount a penalty is raised. Each penalty consists of a 
			subnode 'amount' (penaly amount in US Dollar) and a type:
				'prorated' - to be defined
				'absolute' - to be defined
 		
 	example of a Benchmark SLA definition file: (filename: 
		./config/sla/mutebench_sla_sample)
	
	<?xml version="1.0"?>
	<parameters>
		<benchmark>tpcc</benchmark>
		<window>10</window>
		<sla>
			<name>@STANDARDTENANT#tid@</name>
			<transactions>ALL</transactions>
			<target>@SCHEMA TENANT#tid@</target>
			<metric>LATENCY_AVERAGE</metric>	
			<serviceLevels>
				<sl>
					<metricAmount>1</metricAmount>
					<penaltyAmount>100</penaltyAmount>
				</sl>
				<sl>
					<metricAmount>2</metricAmount>
					<penaltyAmount>1000</penaltyAmount>
				</sl>
			</serviceLevels>
		</sla>
	</parameters>


/*******************************************************************************
 * 3.4  Instructions for editing the Configuration File for a Benchmark Run
 *******************************************************************************/

 	MuTeBench is based on OLTP-Bench and, therefore, reuses the configuration
 	files of OLTP-Bench 
 	(see http://oltpbenchmark.com/wiki/index.php?title=Quickstart#Workload_descriptor)
 	Each OLTP-Bench configuration file can be used for MuTeBench. However we 
 	extended the capabilities for these files with three enhancements:
 	
 	1. Each (!) parameter can be tenant specific by the use of wildcards 
 	   (Section 1.3). For example, the parameter '<rate>@#tid000@</rate>' will
 	   lead to a rate of 1,000 for tenant 1 and a rate of 5,000 for tenant 5.
 	  
 	2. In order to support transaction class aware Service Level Agreements a
 	   DBMS has to know the current transaction name. Not all DBMS support 
 	   explicit definition of transaction starts and transaction names. 
 	   Therefore, we added a parameter <proprietaryTaSyntax>, which should be
 	   used for our extended H2-DBMS only. This parameter is optional and our
 	   proprietary syntax will not be used per default. Our Syntax: 
 	   	- BEGIN WORK <taName>
 		- ROLLBACK WORK <taName> [TO SAVEPOINT <savepointName>]
 		- COMMIT WORK <taName>
 	
 	3. We added a parameter <taSize>. The default transaction size is 1, and all
 	   benchmarks will execute the same transactions as without using taSize. 
 	   If you use taSize 2 they will combine two transactions and so on. 
 	   Example: For the most transactions YCSB benchmark executes only 1 statement. 
 	   With <taSize>10</taSize> it will execute 10 statements per transaction 
 	   instead.
 	   		
	Example (filename: ./config/mutebench_config_sample):
	
	<?xml version="1.0"?>
	<parameters>
	    <!-- Connection details -->
	    <dbtype>h2</dbtype>
	    <driver>org.h2.Driver</driver>
	    <DBUrl>@jdbc:h2:file:/mnt/local/test/tpcc;SCHEMA=tenant#tid;@</DBUrl>
	    <username>@tenant#tid@</username>
	    <password>?</password>
    	<isolation>TRANSACTION_SERIALIZABLE</isolation>
	    <proprietaryTaSyntax>true</proprietaryTaSyntax>
	    
	    <!-- Scale factor is the number of warehouses in TPCC -->
	    <scalefactor>5</scalefactor>
	    
	    <!-- The workload -->
	    <terminals>50</terminals>
	    <works>
	 	 <work>
	          <time>3600</time>
	          <rate>unlimited</rate>
	          <weights>45,43,4,4,4</weights>
	        </work>
		</works>
		
		<!-- TPCC specific -->  
	   	<transactiontypes>
	    	<transactiontype>
	    		<name>NewOrder</name>
	    	</transactiontype>
	    	<transactiontype>
	    		<name>Payment</name>
	    	</transactiontype>
	    	<transactiontype>
	    		<name>OrderStatus</name>
	    	</transactiontype>
	    	<transactiontype>
	    		<name>Delivery</name>
	    	</transactiontype>
	    	<transactiontype>
	    		<name>StockLevel</name>
	    	</transactiontype>
	   	</transactiontypes>	
	</parameters>	