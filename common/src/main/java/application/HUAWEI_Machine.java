package application;

public class HUAWEI_Machine extends Platform {

	private static final long serialVersionUID = 2511492570058016274L;

	public HUAWEI_Machine() {
		this.latency_L2 = 11.2;//measured latency in ns for each cache line sized tuple access.
		this.latency_LLC = 50;//measured latency in ns for each cache line sized tuple access.
		this.latency_LOCAL_MEM = 110;//measured latency in ns for each cache line access.
		this.cache_line = 64.0;//bytes
		this.CLOCK_RATE = 1.2;//1.2GHz ... 2.5 cycles per nanosecond, in real, it's about 1.2. so we use UNHALTED_REFERENCE_CYCLES that does not affect.
		this.num_socket = 8;
		this.CoresPerSocket = 36;//36 cores per socket
		this.num_cores = 288;
		bandwidth_map = new double[][]{  //in MB/s
				{55607.1, 13482.4, 13161.4, 14636.7, 6551.4, 6532.9, 5963.2, 5941.0},
				{13426.7, 55401.0, 14538.7, 13182.1, 6642.1, 6603.0, 5907.5, 5893.8},
				{13145.9, 14622.5, 55257.4, 13470.2, 5980.0, 5949.1, 6543.7, 6527.6},
				{14569.4, 13182.4, 13403.7, 55559.4, 5907.3, 5885.7, 6637.8, 6610.2},
				{6580.5, 6622.0, 5890.6, 5893.2, 56023.5, 13486.7, 13179.6, 14643.9},
				{6537.9, 6523.0, 5962.6, 5979.3, 13446.2, 55629.8, 14552.2, 13177.8},
				{5897.4, 5894.0, 6614.0, 6630.6, 13166.9, 14633.0, 55604.3, 13448.3},
				{5948.2, 5957.0, 6540.9, 6528.3, 14571.6, 13196.9, 13409.5, 55344.1},
		};

		latency_map = new double[][]{// in ns
				{0, 119.3, 172.0, 120.4, 395.6, 413.7, 482.2, 476.4},
				{116.0, 0, 119.1, 163.5, 393.8, 409.9, 481.3, 475.8},
				{170.3, 119.0, 0, 118.1, 479.9, 474.3, 393.4, 413.1},
				{116.7, 165.6, 118.1, 0, 481.1, 471.8, 393.6, 407.7},
				{408.9, 393.5, 474.7, 479.3, 0, 118.5, 170.9, 120.2},
				{411.7, 394.6, 476.3, 482.1, 118.6, 0, 119.4, 171.0},
				{476.6, 481.7, 405.3, 394.3, 170.4, 119.3, 0, 118.4},
				{478.7, 482.3, 413.4, 395.2, 120.5, 171.8, 118.4, 0},
//
//				{130.3, 307.7, 361.1, 307.3, 481.7, 481.2, 544.4, 548.0},
//				{307.7, 130.3, 307.4, 360.2, 482.2, 481.8, 550.4, 550.4},
//				{361.1, 307.4, 130.3, 307.4, 541.8, 546.5, 482.4, 482.3},
//				{307.3, 360.2, 307.4, 130.3, 550.7, 549.6, 482.2, 482.6},
//				{481.7, 482.2, 541.8, 550.7, 130.3, 307.3, 359.6, 315.2},
//				{481.2, 481.8, 546.5, 549.6, 307.3, 130.3, 307.5, 367.0},
//				{544.4, 550.4, 482.4, 482.2, 359.6, 307.5, 130.3, 311.1},
//				{548.0, 550.4, 482.6, 482.6, 315.2, 367.0, 311.1, 130.3},

		};
	}
}
