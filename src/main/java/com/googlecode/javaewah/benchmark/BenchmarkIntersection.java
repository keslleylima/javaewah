package com.googlecode.javaewah.benchmark;

import java.text.DecimalFormat;
import com.googlecode.javaewah.*;

/*
 * Copyright 2009-2013, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz and Owen Kaser
 * Licensed under the Apache License, Version 2.0.
 */
/**
 * To benchmark the logical and (intersection) aggregate. 
 */
public class BenchmarkIntersection {

	@SuppressWarnings("javadoc")
	public static void main(String args[]) {
		test(10, 18, 1);
	}

	@SuppressWarnings({ "javadoc"})
	public static void test(int N, int nbr, int repeat) {
		long bogus = 0;

		DecimalFormat df = new DecimalFormat("0.###");
		ClusteredDataGenerator cdg = new ClusteredDataGenerator();
		for (int sparsity = 1; sparsity < 30 - nbr; sparsity++) {
			for (int times = 0; times < 2; ++times) {
				String line = "";
				long bef, aft;
				line += sparsity;
				int[][] data = new int[N][];
				int Max = (1 << (nbr + sparsity));
				int[] inter = cdg.generateClustered(1 << (nbr/2), Max);
				for (int k = 0; k < N; ++k) 
					data[k] = Benchmark.unite2by2(cdg.generateClustered(1 << nbr, Max),inter);
				// building
				EWAHCompressedBitmap[] ewah = new EWAHCompressedBitmap[N];
				for (int k = 0; k < N; ++k) {
					ewah[k] = new EWAHCompressedBitmap();
					for (int x = 0; x < data[k].length; ++x) {
						ewah[k].set(data[k][x]);
					}
					data[k] = null;
				}
				// sanity check
				if (true) {
					EWAHCompressedBitmap answer = ewah[0].and(ewah[1]);
					for (int k = 2; k < ewah.length; ++k)
						answer = answer.and(ewah[k]);

					EWAHCompressedBitmap ewahand = EWAHCompressedBitmap.and(ewah);
					if (!answer.equals(ewahand))
						throw new RuntimeException(
								"bug EWAHCompressedBitmap.and");
					EWAHCompressedBitmap ewahand2 = FastAggregation
							.bufferedand(65536,ewah);
					if (!ewahand.equals(ewahand2))
						throw new RuntimeException(
								"bug FastAggregation.bufferedand ");

				}

				// logical or
				bef = System.currentTimeMillis();
				for (int r = 0; r < repeat; ++r)
					for (int k = 0; k < N; ++k) {
						EWAHCompressedBitmap ewahor = ewah[0];
						for (int j = 1; j < k + 1; ++j) {
							ewahor = ewahor.and(ewah[j]);
						}
					}
				aft = System.currentTimeMillis();
				line += "\t" + df.format((aft - bef) / 1000.0);

				// fast logical or
				bef = System.currentTimeMillis();
				for (int r = 0; r < repeat; ++r)
					for (int k = 0; k < N; ++k) {
						EWAHCompressedBitmap[] ewahcp = new EWAHCompressedBitmap[k + 1];
						for (int j = 0; j < k + 1; ++j) {
							ewahcp[j] = ewah[j];
						}
						EWAHCompressedBitmap ewahor = EWAHCompressedBitmap
								.and(ewahcp);
						bogus += ewahor.sizeInBits();
					}
				aft = System.currentTimeMillis();
				line += "\t" + df.format((aft - bef) / 1000.0);
				// fast logical or
				bef = System.currentTimeMillis();
				for (int r = 0; r < repeat; ++r)
					for (int k = 0; k < N; ++k) {
						EWAHCompressedBitmap[] ewahcp = new EWAHCompressedBitmap[k + 1];
						for (int j = 0; j < k + 1; ++j) {
							ewahcp[j] = ewah[j];
						}
						EWAHCompressedBitmap ewahor = FastAggregation
								.bufferedand(65536,ewahcp);
						bogus += ewahor.sizeInBits();
					}
				aft = System.currentTimeMillis();

				line += "\t" + df.format((aft - bef) / 1000.0);
				// fast logical or
				bef = System.currentTimeMillis();
				for (int r = 0; r < repeat; ++r)
					for (int k = 0; k < N; ++k) {
						IteratingRLW[] ewahcp = new IteratingRLW[k + 1];
						for (int j = 0; j < k + 1; ++j) {
							ewahcp[j] = new IteratingBufferedRunningLengthWord(
									ewah[j]);
						}
						IteratingRLW ewahor = IteratorAggregation.bufferedand(ewahcp);
						int wordcounter = IteratorUtil.cardinality(ewahor);
						bogus += wordcounter;
					}
				aft = System.currentTimeMillis();

				line += "\t" + df.format((aft - bef) / 1000.0);

				System.out
						.println("# times for: 2by2 EWAHCompressedBitmap.and bufferedand iterator-bufferedand");

				System.out.println(line);
			}
			System.out.println("# bogus =" + bogus);

		}
	}
}
