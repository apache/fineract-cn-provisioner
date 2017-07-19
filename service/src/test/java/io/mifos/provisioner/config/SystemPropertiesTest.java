package io.mifos.provisioner.config;

import io.mifos.core.lang.security.RsaKeyPairFactory;
import io.mifos.core.test.domain.ValidationTest;
import io.mifos.core.test.domain.ValidationTestCase;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Myrle Krantz
 */
public class SystemPropertiesTest extends ValidationTest<SystemProperties> {
  private static final RsaKeyPairFactory.KeyPairHolder keyPairHolder = RsaKeyPairFactory.createKeyPair();

  public SystemPropertiesTest(ValidationTestCase<SystemProperties> testCase) {
    super(testCase);
  }

  @Override
  protected SystemProperties createValidTestSubject() {
    final SystemProperties ret = new SystemProperties();
    ret.getPrivateKey().setModulus(keyPairHolder.getPrivateKeyMod());
    ret.getPrivateKey().setExponent(keyPairHolder.getPrivateKeyExp());
    ret.getPublicKey().setTimestamp(keyPairHolder.getTimestamp());
    ret.getPublicKey().setModulus(keyPairHolder.getPublicKeyMod());
    ret.getPublicKey().setExponent(keyPairHolder.getPublicKeyExp());
    return ret;
  }

  @Parameterized.Parameters
  public static Collection testCases() {
    final Collection<ValidationTestCase> ret = new ArrayList<>();
    ret.add(new ValidationTestCase<SystemProperties>("basicCase")
        .adjustment(x -> {})
        .valid(true));
    ret.add(new ValidationTestCase<SystemProperties>("missing private modulus")
        .adjustment(x -> x.getPrivateKey().setModulus(null))
        .valid(false));
    ret.add(new ValidationTestCase<SystemProperties>("mismatched keys")
        .adjustment(x -> {
          final RsaKeyPairFactory.KeyPairHolder keyPairHolder = RsaKeyPairFactory.createKeyPair();
          x.getPrivateKey().setModulus(keyPairHolder.getPrivateKeyMod());
          x.getPrivateKey().setExponent(keyPairHolder.getPrivateKeyExp());
        })
        .valid(false));
    ret.add(new ValidationTestCase<SystemProperties>("missing timestamp")
        .adjustment(x -> x.getPublicKey().setTimestamp(null))
        .valid(false));
    return ret;
  }

}