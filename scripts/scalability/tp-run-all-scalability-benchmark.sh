#!/bin/bash
source ../dir.sh || exit
function ResetParameters() {
    app="TollProcessing"
    checkpointInterval=40960
    tthread=24
    scheduler="OG_NS_A"
    defaultScheduler="OG_NS_A"
    CCOption=3 #TSTREAM
    complexity=8000
    NUM_ITEMS=491520
    abort_ratio=3000
    overlap_ratio=10
    key_skewness=30
    isDynamic=1
    workloadType="default,unchanging,unchanging,unchanging"
  # workloadType="default,unchanging,unchanging,unchanging,Up_abort,Down_abort,unchanging,unchanging"
  # workloadType="default,unchanging,unchanging,unchanging,Up_skew,Up_skew,Up_skew,Up_PD,Up_PD,Up_PD,Up_abort,Up_abort,Up_abort"
    schedulerPool="OG_NS_A,OG_NS"
    rootFilePath="${RSTDIR}"
    shiftRate=1
    multicoreEvaluation=1
    maxThreads=24
    totalEvents=`expr $checkpointInterval \* $maxThreads \* 4 \* $shiftRate`

    snapshotInterval=4
    arrivalControl=1
    arrivalRate=300
    FTOption=0
    isRecovery=0
    isFailure=0
    failureTime=2500000
    measureInterval=100
    compressionAlg="None"
    isSelective=0
    maxItr=0
}

function runApplication() {
  echo "java -Xms300g -Xmx300g -Xss100M -XX:+PrintGCDetails -Xmn200g -XX:+UseG1GC -jar -d64 ${JAR} \
              --app $app \
              --NUM_ITEMS $NUM_ITEMS \
              --tthread $tthread \
              --scheduler $scheduler \
              --defaultScheduler $defaultScheduler \
              --checkpoint_interval $checkpointInterval \
              --CCOption $CCOption \
              --complexity $complexity \
              --abort_ratio $abort_ratio \
              --overlap_ratio $overlap_ratio \
              --key_skewness $key_skewness \
              --rootFilePath $rootFilePath \
              --isDynamic $isDynamic \
              --totalEvents $totalEvents \
              --shiftRate $shiftRate \
              --workloadType $workloadType \
              --schedulerPool $schedulerPool \
              --multicoreEvaluation $multicoreEvaluation \
              --maxThreads $maxThreads \
              --snapshotInterval $snapshotInterval \
              --arrivalControl $arrivalControl \
              --arrivalRate $arrivalRate \
              --FTOption $FTOption \
              --isRecovery $isRecovery \
              --isFailure $isFailure \
              --failureTime $failureTime \
              --measureInterval $measureInterval \
              --compressionAlg $compressionAlg \
              --isSelective $isSelective \
              --maxItr $maxItr"
    java -Xms300g -Xmx300g -Xss100M -XX:+PrintGCDetails -Xmn200g -XX:+UseG1GC -jar -d64 $JAR \
      --app $app \
      --NUM_ITEMS $NUM_ITEMS \
      --tthread $tthread \
      --scheduler $scheduler \
      --defaultScheduler $defaultScheduler \
      --checkpoint_interval $checkpointInterval \
      --CCOption $CCOption \
      --complexity $complexity \
      --abort_ratio $abort_ratio \
      --overlap_ratio $overlap_ratio \
      --key_skewness $key_skewness \
      --rootFilePath $rootFilePath \
      --isDynamic $isDynamic \
      --totalEvents $totalEvents \
      --shiftRate $shiftRate \
      --workloadType $workloadType \
      --schedulerPool $schedulerPool \
      --multicoreEvaluation $multicoreEvaluation \
      --maxThreads $maxThreads \
      --snapshotInterval $snapshotInterval \
      --arrivalControl $arrivalControl \
      --arrivalRate $arrivalRate \
      --FTOption $FTOption \
      --isRecovery $isRecovery \
      --isFailure $isFailure \
      --failureTime $failureTime \
      --measureInterval $measureInterval \
      --compressionAlg $compressionAlg \
      --isSelective $isSelective \
      --maxItr $maxItr
}
function withRecovery() {
    isFailure=1
    isRecovery=0
    runApplication
    sleep 2s
    isFailure=0
    isRecovery=1
    runApplication
}
function multicoreEvaluation() {
  #  tthread=24
  #  snapshotInterval=4
  #  withRecovery
  #  sleep 2s

   tthread=12
   snapshotInterval=8
   withRecovery
   sleep 2s

   tthread=8
   snapshotInterval=12
   withRecovery
   sleep 2s

   tthread=4
   snapshotInterval=24
   withRecovery
   sleep 2s

   tthread=1
   snapshotInterval=96
   withRecovery
   sleep 2s
}

function application_runner() {
 ResetParameters
 app=TollProcessing
 for FTOption in 1 5 6
 do
 multicoreEvaluation
 done
}
application_runner
