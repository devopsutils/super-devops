package com.wl4g.devops.tool.common.cli.ssh2;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import com.wl4g.devops.tool.common.cli.ssh2.Ssh2Holders;
import com.wl4g.devops.tool.common.cli.ssh2.SshjHolder;

/**
 * @author vjay
 * @date 2020-01-08 17:16:00
 */
public class SshjHolderTests {

	public static void main(String[] args) throws Exception {
		// Test execute command
		// SshjUtils.SshExecResponse sshExecResponse =
		// SshjUtils.execWithSsh2("10.0.0.160", "root",
		// privateKey.toCharArray(), "ls", 60000);
		// System.out.println("success="+sshExecResponse.getMessage());
		// System.out.println("fail="+sshExecResponse.getErrmsg());
		// System.out.println("exitCode="+sshExecResponse.getExitCode());

		long t1 = System.currentTimeMillis();
		// Test upload file
		String loaclFile = "/Users/vjay/Downloads/bigFile.txt";
		makeFile(loaclFile);
		Ssh2Holders.getInstance(SshjHolder.class).scpPutFile("10.0.0.160", "root", privateKey.toCharArray(), new File(loaclFile),
				"/root/testssh/devops-0107.sql");
		long t2 = System.currentTimeMillis();
		System.out.println(t2 - t1);
	}

	private static void makeFile(String fileName) throws IOException {
		if (!new File(fileName).exists()) {
			try (FileWriter fileWriter = new FileWriter(fileName);) {
				for (int i = 0; i <= 999999; i++) {
					fileWriter.write(
							"1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890");
				}
			}
		}
	}

	private static final String privateKey = "-----BEGIN RSA PRIVATE KEY-----\n"
			+ "MIIEpQIBAAKCAQEAwawifYZlHNdmkdMmXdi6wslkfvvAVjGo4cBPtrOFonD0Paex\n"
			+ "tVRckfkj6rCu4IkKOq6HFBBf1peYVHojLFUm4FGC+YatxoLcdExBj8A/oMVsWN8a\n"
			+ "ZWv5RH0lqUPZyuefqIrD+pos0R1hJtEDh5cKKT+Ae7kOP2+pX0QeGu0F/z9jozPo\n"
			+ "PiM5DaoM0xaDqhmn1dnY03X3TAY8/V9Oy1zSRslXoiF2EmfTiaHBlvCeK5WhCiMd\n"
			+ "5Xfn26Eiw3RBePh/eGiXjgv2ILZA7pnoINCa+PXI6VW6mthHQ8GJ7w+2afCGZBun\n"
			+ "hxpGPEKih8YGNHBlGKUPale0pPMqI703iENZrQIDAQABAoIBAQCD2/qvk+0Lsev3\n"
			+ "pNceVgzxycROYIEXLkBZU2Hydk+pxVXFFIN9fa55BDNb+mdWIHeCdIkrM+rMY/Im\n"
			+ "sfF4oZEScOzHjtaJrVcDJ1gL00x+3WtjJqMGInlYFAysLbH+36xoR/IekRGqXmJi\n"
			+ "1zOcAU29v6pukhQNRK0AW5RTqMTIfuQeR7Utpq4QNk8BSfDKJcyVoiTpaJ3Pv4AG\n"
			+ "rPkIgtIqtmDJmp6MbMVtJA8AyxYrAiJbHntVeRddj2NkrScK/J4RG+5U/9Jj5cpB\n"
			+ "VA21C7pt4lvUVuAPQ+esFLiwxBlIfW+sS5jaKCbJaeBL78xOiaLBJNWfrGR4USdu\n"
			+ "k0P8o/sBAoGBAO6YeX2TDyVH/8XsSq9nPs+hu85wMqWohswG9R6oxcurVJLLKjdd\n"
			+ "vqwpFaOUaQHcZr7iN2jGWqxxjFRW5qnwevk8uyXs9joSKV2C7KUdxuP9dtuqJNyl\n"
			+ "+p1qN57+J69WXQNJoMKmiICK43fW89CoNucqJqrmsLzBkQiFmtR83ORxAoGBAM/M\n"
			+ "xBTaVPrJYXxx10TZo+NcN5dv4FfDmK/Uz6v2F4y8xoRxpde8yeo0xj9CM8J2SX8w\n"
			+ "712GLWI03m8RsYg/g2qRueK34lrA0eJ8GGUV2SJOqOQqlv1FJJVDp3658Cm0IyDT\n"
			+ "CvyHJyrGmJljUhTgnwMuuzcp1wAfenWFp6OaAfb9AoGAXZMxOr29V+rH9nD4zZgZ\n"
			+ "e0c8J/e69VuGGmi0I+UfRgSY88V4diRvDohCc1hWYqN1LHH+Nzpr/2u9FKrMZmPp\n"
			+ "ZuyZnYM1Aoty67jYZN2rzmju/7HYKS1zf99TlyiomcyuSAbNZOn5aSiPk8Wa8/+1\n"
			+ "IK5YYfh94lmsLwJvOd0KqRECgYEAxUa22L02dCh/Pm+tWRXt+0lvFXwG1gtBh5xX\n"
			+ "0/97+Aa3yMFEGv6GCq0zkJa/INy/hdrlRDrAFz3t9jAsBReXIbNbcBv27wWjvIrn\n"
			+ "dgA59dILkSHF2oir5HEoMK1BjbYQq3bwNTHyQy/ra6PZJyzgiVryLbqw/NLlpXDP\n"
			+ "6Aer2dkCgYEAwKka1EYm5/N4krwsVvNBWD4Xgt4dtkGkQYkhyXZIqGTLFntIdVig\n"
			+ "jKoQ6kaFTPaSST4kWNoXxNWvBDjarOriPa//St+l5fsEjfhjF1CfCS5aKvKfIwmP\n"
			+ "jZss7kAharoCjXmxdyqPBEjJPHMts7d93olfGDGCvFrZnEfuD+zUcmU=\n" + "-----END RSA PRIVATE KEY-----";

}
