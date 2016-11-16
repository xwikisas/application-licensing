/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package com.xwiki.licensing.test.internal;

import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.crypto.AsymmetricKeyFactory;
import org.xwiki.crypto.BinaryStringEncoder;
import org.xwiki.crypto.params.cipher.asymmetric.PrivateKeyParameters;
import org.xwiki.crypto.pkix.CertificateFactory;
import org.xwiki.crypto.pkix.CertificateProvider;
import org.xwiki.crypto.pkix.params.CertifiedKeyPair;
import org.xwiki.crypto.pkix.params.CertifiedPublicKey;
import org.xwiki.crypto.pkix.params.PrincipalIndentifier;
import org.xwiki.crypto.pkix.params.x509certificate.X509CertifiedPublicKey;
import org.xwiki.crypto.signer.SignerFactory;
import org.xwiki.properties.converter.Converter;

import com.xwiki.licensing.License;
import com.xwiki.licensing.SignedLicense;
import com.xwiki.licensing.test.SignedLicenseTestUtils;

/**
 * Default implementation of {@link SignedLicenseTestUtils}.
 *
 * @version $Id$
 */
@Component
@Singleton
public class DefaultSignedLicenseTestUtils implements SignedLicenseTestUtils, Initializable
{
    private static final String DSA_PRIVATE_KEY =
        // Link to decoded ASN.1: https://goo.gl/n6abEQ
        "MIIBTAIBADCCASwGByqGSM44BAEwggEfAoGBANQ9Oa1j9sWAhdXNyqz8HL/bA/e"
            + "d2VrBw6TPkgMyV1Upix58RSjOHMQNrgemSGkb80dRcLqVDYbI3ObnIJh83Zx6ze"
            + "aTpvUohGLyTa0F7UY15LkbJpyz8WFJaVykH85nz3Zo6Md9Z4X95yvF1+h9qYuak"
            + "jWcHW31+pN4u3cJNg5FAhUAj986cVG9NgWgWzFVSLbB9pEPbFUCgYEAmQrZFH3M"
            + "X5CLX/5vDvxyTeeRPZHLWc0ik3GwrIJExuVrOkuFInpx0aVbuJTxrEnY2fuc/+B"
            + "yj/F56DDO31+qPu7ZxbSvvD33OOk8eFEfn+Hia3QmA+dGrhUqoMpfDf4/GBgJhn"
            + "yQtFzMddHmYB0QnS9yX1n6DOWj/CSX0PvrlMYEFwIVAIO1GUQjAddL4btiFQnhe"
            + "N4fxBTa";

    private static final String DSA_PUBLIC_KEY =
        // Link to decoded ASN.1: https://goo.gl/0fLEBU
        "MIIBtzCCASwGByqGSM44BAEwggEfAoGBANQ9Oa1j9sWAhdXNyqz8HL/bA/ed2VrB"
            + "w6TPkgMyV1Upix58RSjOHMQNrgemSGkb80dRcLqVDYbI3ObnIJh83Zx6zeaTpvUo"
            + "hGLyTa0F7UY15LkbJpyz8WFJaVykH85nz3Zo6Md9Z4X95yvF1+h9qYuakjWcHW31"
            + "+pN4u3cJNg5FAhUAj986cVG9NgWgWzFVSLbB9pEPbFUCgYEAmQrZFH3MX5CLX/5v"
            + "DvxyTeeRPZHLWc0ik3GwrIJExuVrOkuFInpx0aVbuJTxrEnY2fuc/+Byj/F56DDO"
            + "31+qPu7ZxbSvvD33OOk8eFEfn+Hia3QmA+dGrhUqoMpfDf4/GBgJhnyQtFzMddHm"
            + "YB0QnS9yX1n6DOWj/CSX0PvrlMYDgYQAAoGAJvnuTm8oI/RRI2tiZHtPkvSQaA3F"
            + "P4PRsVx6z1oIGg9OAxrtSS/aiQa+HWFg7fjHlMJ30Vh0yqt7igj70jaLGyDvr3MP"
            + "DyiO++72IiGUluc6yHg6m9cQ53eeJt9i44LJfTOw1S3YMU1ST7alokSnJRTICp5W"
            + "By0m1scwheuTo0E=";

    private static final String V3_CA_CERT =
        // Generated from BcX509CertificateGeneratorFactoryTest#testGenerateIntermediateCertificateVersion3()
        // caCertificate with adapted validity
        // Link to decoded ASN.1: https://goo.gl/XP56Ct
        "MIIDEDCCAfigAwIBAgIPXnmirmTeiBhYy1/i/twrMA0GCSqGSIb3DQEBBQUAMBIx"
            + "EDAOBgNVBAMMB1Rlc3QgQ0EwHhcNMTUwOTEwMTAwMDAwWhcNNDkxMjMxMTEwMDAw"
            + "WjASMRAwDgYDVQQDDAdUZXN0IENBMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIB"
            + "CgKCAQEAwpo4pv95YpCeFReeuFMsoKj4yF/89PdACJi5dHwy1JCMglU0ndfMCd2s"
            + "a9aoy7UoDT1nrdKbksThR9AMxSkQiTo1JEEVPIgJM+uDCwH/ca8eJwRAnIqngfeN"
            + "bNNZC7RvAv1KgtzvUsB0nYbVKVFPFf/dUog9PcjSbJzg4t1qjN6C4NUGMyoaGg9p"
            + "laNsSS0P0F6q9lyHOYtjJaX6ukuSTWxQ/yPlBYQVJQTbgzhFe7bSLdnhTBFLH5An"
            + "rLRqNGKEGDqUyz3DxkUuzJ0mNQi1hlGKmjEp3rNJ0vJHtjzw+ENLLgGc+JrJ4Tbn"
            + "2p7HLLTPLXDbbGReoY9Cb22aJPPbMwIDAQABo2MwYTAfBgNVHSMEGDAWgBSIFnbz"
            + "7l0iZJUJMLz0yIaYE84VYTAdBgNVHQ4EFgQUiBZ28+5dImSVCTC89MiGmBPOFWEw"
            + "DwYDVR0TAQH/BAUwAwEB/zAOBgNVHQ8BAf8EBAMCAQYwDQYJKoZIhvcNAQEFBQAD"
            + "ggEBAEQnUNI1REuJGNrnEIxqLJX6w8TmgUp6g+4ytqh8fZoy1tHIH6+dhPhP87Gk"
            + "E4QVfq5AZDb/mMuZCdGjOAaWqXsaUfJNFcJcrbKxnu0MBbfcPrHkhTTlDpoPV+N7"
            + "P+iLBHJB3IAVcic+b/xXqeUn50Yp7VmfIVYzrIS3diA57hHntm72eMsYHLjuToxh"
            + "QD9E/PVo3eqAH2MW71+RY0X75gZytu3in9/1v0+IbFBeTG7KrVxMAt+x2pE7CybA"
            + "pNxs5wkFxwU0wF2jjTgef6ivAS3gn+KOk9YR1xpMk5FkHAjwCUWEve8GKvEIU/83"
            + "0z4rffrSX2Y9mYm4gF/GtjAkgLA=";

    private static final String V3_ITERCA_CERT =
        // Generated from BcX509CertificateGeneratorFactoryTest#testGenerateIntermediateCertificateVersion3()
        // interCAcert with adapted validity
        // Link to decoded ASN.1: https://goo.gl/ahbPjf
        "MIID4jCCAsqgAwIBAgIRALpeBEAtiuOWnOU9EQEPV5wwDQYJKoZIhvcNAQEFBQAw"
            + "EjEQMA4GA1UEAwwHVGVzdCBDQTAeFw0xNTA5MTAxMDAwMDBaFw00OTEyMzExMTAw"
            + "MDBaMB8xHTAbBgNVBAMMFFRlc3QgSW50ZXJtZWRpYXRlIENBMIIBtzCCASwGByqG"
            + "SM44BAEwggEfAoGBALjHlfmpKj8BiEfekiLTnbYdZlo5Hz6E2dAjx+ryqv3jeGYb"
            + "PTxh+pxrD0MIIUKF+3o8Y+TBwBpbKnnZ/G2T/P6QXs8+l7H7Q4CUJKShdQ+PhpK8"
            + "JXYaICN4VAtKsP4PVhBMWLw/3VANh67JDwZz1Oa5soci3dAVQDWN8mc4PdbhAhUA"
            + "oWrfRj14AUQT759T/Men1dQ9o0ECgYEAgWPlEWkpvgfkCvyBMRiRWchS0suOUUL5"
            + "RyqYKmVFpDE2aKRMMFO5owlluJ1lm57f4zaddY8zAsT72tv0tTxz7nFAAPoX4QPO"
            + "cSxYYapvEGRZklJRU4qrrXOPlXTia6jsWlgjnMaJ43zCBXteK2AdZ2DF7Yr9UPRu"
            + "NukIzSYc4pcDgYQAAoGAEH/cX4auYYjapwPvipulmUPLPB9GTPcZfcefLYH4FlAs"
            + "/W1/vfer1kGZL/+urSu+5D/FonOGNE9VRnLhVO4SyOremfJTO0ZLA7w5ciQwcQRx"
            + "wXX3vvYzxtiFA2H7G7SHVcg8GDzyikHePQnyDwjgXf2C8dxcyasUA5FJb62YKo2j"
            + "gZAwgY0wSAYDVR0jBEEwP4AUiBZ28+5dImSVCTC89MiGmBPOFWGhFqQUMBIxEDAO"
            + "BgNVBAMMB1Rlc3QgQ0GCD155oq5k3ogYWMtf4v7cKzAdBgNVHQ4EFgQUbrrdokxq"
            + "hLVQHnfZkpK1PN7kUbowEgYDVR0TAQH/BAgwBgEB/wIBADAOBgNVHQ8BAf8EBAMC"
            + "AQYwDQYJKoZIhvcNAQEFBQADggEBALuttCfxVloX+cVZw97ny2C0Gx1gF2iQXPUq"
            + "7QjsTs9xWB2X2j9hMYS8x4oB8x2w59PRZpdtdlFvjxeLR9xrX1yFeqcGMZ2opEqx"
            + "htFhSE28Inv+A+VWo/Je1T986XEgGMrIgPkW46Bc8xsNy63WHdcpj7U9xWU2Qcs8"
            + "EPzTNV3ibevNdCS6bwXEpgn2fSV7dsscaZGt9O43Co2iyGYqXZXqjRXOJkQmLFc/"
            + "p5xPpsPhFXERE65Ihdtl17XLiIt88mvOyfNl/X9c28lLf0+hlEqnmE1CDrvitKAy"
            + "hHU7w1nXlQZZz6RWvilHqn0NkT3vTYhQBVXq5XiQwT8xLBjJJtw=";

    private static final String V3_CERT =
        // Generated from BcX509CertificateGeneratorFactoryTest#testGenerateIntermediateCertificateVersion3()
        // certificate with adapted validity
        // Link to decoded ASN.1: https://goo.gl/OPAFci
        "MIIDMjCCAvCgAwIBAgIRAKm/hze1CY3FuwXuKd86eXgwCwYHKoZIzjgEAwUAMB8x"
            + "HTAbBgNVBAMMFFRlc3QgSW50ZXJtZWRpYXRlIENBMB4XDTE1MDkxMDEwMDAwMFoX"
            + "DTQ5MTIzMTExMDAwMFowGjEYMBYGA1UEAwwPVGVzdCBFbmQgRW50aXR5MIIBtzCC"
            + "ASwGByqGSM44BAEwggEfAoGBANQ9Oa1j9sWAhdXNyqz8HL/bA/ed2VrBw6TPkgMy"
            + "V1Upix58RSjOHMQNrgemSGkb80dRcLqVDYbI3ObnIJh83Zx6zeaTpvUohGLyTa0F"
            + "7UY15LkbJpyz8WFJaVykH85nz3Zo6Md9Z4X95yvF1+h9qYuakjWcHW31+pN4u3cJ"
            + "Ng5FAhUAj986cVG9NgWgWzFVSLbB9pEPbFUCgYEAmQrZFH3MX5CLX/5vDvxyTeeR"
            + "PZHLWc0ik3GwrIJExuVrOkuFInpx0aVbuJTxrEnY2fuc/+Byj/F56DDO31+qPu7Z"
            + "xbSvvD33OOk8eFEfn+Hia3QmA+dGrhUqoMpfDf4/GBgJhnyQtFzMddHmYB0QnS9y"
            + "X1n6DOWj/CSX0PvrlMYDgYQAAoGAJvnuTm8oI/RRI2tiZHtPkvSQaA3FP4PRsVx6"
            + "z1oIGg9OAxrtSS/aiQa+HWFg7fjHlMJ30Vh0yqt7igj70jaLGyDvr3MPDyiO++72"
            + "IiGUluc6yHg6m9cQ53eeJt9i44LJfTOw1S3YMU1ST7alokSnJRTICp5WBy0m1scw"
            + "heuTo0GjgbAwga0wSgYDVR0jBEMwQYAUbrrdokxqhLVQHnfZkpK1PN7kUbqhFqQU"
            + "MBIxEDAOBgNVBAMMB1Rlc3QgQ0GCEQC6XgRALYrjlpzlPREBD1ecMB0GA1UdDgQW"
            + "BBSdIuxgWLG45Mk01RHaIRUu2RadHjAOBgNVHQ8BAf8EBAMCBJAwEwYDVR0lBAww"
            + "CgYIKwYBBQUHAwQwGwYDVR0RBBQwEoEQdGVzdEBleGFtcGxlLmNvbTALBgcqhkjO"
            + "OAQDBQADLwAwLAIUe6xUKxqsupL5MWKKdsaY0FiGWVgCFHG3MW1tmgQ6CChIdA9K"
            + "5fBjULkT";

    private static final String SIGNED_LICENSE =
        "MIAGCSqGSIb3DQEHAqCAMIACAQExCzAJBgUrDgMCGgUAMIAGCSqGSIb3DQEHAaCA"
            + "JIAEggOVPD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiIHN0YW5k"
            + "YWxvbmU9InllcyI/Pgo8bGljZW5zZSB4bWxucz0iaHR0cDovL3d3dy54d2lraS5j"
            + "b20vbGljZW5zZSIgaWQ9IjAwMDAwMDAwLTAwMDAtMDAwMC0wMDAwLTAwMDAwMDAw"
            + "MDAwMCI+CiAgICA8bW9kZWxWZXJzaW9uPjEuMC4wPC9tb2RlbFZlcnNpb24+CiAg"
            + "ICA8dHlwZT5QQUlEPC90eXBlPgogICAgPGxpY2Vuc2VkPgogICAgICAgIDxmZWF0"
            + "dXJlcz4KICAgICAgICAgICAgPGZlYXR1cmU+CiAgICAgICAgICAgICAgICA8aWQ+"
            + "dGVzdC1hcGk8L2lkPgogICAgICAgICAgICAgICAgPHZlcnNpb24+Mi4wPC92ZXJz"
            + "aW9uPgogICAgICAgICAgICA8L2ZlYXR1cmU+CiAgICAgICAgICAgIDxmZWF0dXJl"
            + "PgogICAgICAgICAgICAgICAgPGlkPnRlc3QtdWk8L2lkPgogICAgICAgICAgICAg"
            + "ICAgPHZlcnNpb24+MS4wPC92ZXJzaW9uPgogICAgICAgICAgICA8L2ZlYXR1cmU+"
            + "CiAgICAgICAgPC9mZWF0dXJlcz4KICAgIDwvbGljZW5zZWQ+CiAgICA8cmVzdHJp"
            + "Y3Rpb25zPgogICAgICAgIDxpbnN0YW5jZXM+CiAgICAgICAgICAgIDxpbnN0YW5j"
            + "ZT4xMTExMTExMS0yMjIyLTMzMzMtNDQ0NC01NTU1NTU1NTU1NTU8L2luc3RhbmNl"
            + "PgogICAgICAgICAgICA8aW5zdGFuY2U+NjY2NjY2NjYtNzc3Ny04ODg4LTk5OTkt"
            + "MDAwMDAwMDAwMDAwPC9pbnN0YW5jZT4KICAgICAgICA8L2luc3RhbmNlcz4KICAg"
            + "ICAgICA8ZXhwaXJlPjE5NzEtMDEtMDFUMDE6MDA6MDArMDE6MDA8L2V4cGlyZT4K"
            + "ICAgICAgICA8dXNlcnM+MTAwPC91c2Vycz4KICAgIDwvcmVzdHJpY3Rpb25zPgog"
            + "ICAgPGxpY2VuY2VlPgogICAgICAgIDxuYW1lPnVzZXI8L25hbWU+CiAgICAgICAg"
            + "PGVtYWlsPnVzZXJAZXhhbXBsZS5jb208L2VtYWlsPgogICAgPC9saWNlbmNlZT4K"
            + "PC9saWNlbnNlPgoAAAAAAACggDCCAxAwggH4oAMCAQICD155oq5k3ogYWMtf4v7c"
            + "KzANBgkqhkiG9w0BAQUFADASMRAwDgYDVQQDDAdUZXN0IENBMB4XDTE1MDkxMDEw"
            + "MDAwMFoXDTQ5MTIzMTExMDAwMFowEjEQMA4GA1UEAwwHVGVzdCBDQTCCASIwDQYJ"
            + "KoZIhvcNAQEBBQADggEPADCCAQoCggEBAMKaOKb/eWKQnhUXnrhTLKCo+Mhf/PT3"
            + "QAiYuXR8MtSQjIJVNJ3XzAndrGvWqMu1KA09Z63Sm5LE4UfQDMUpEIk6NSRBFTyI"
            + "CTPrgwsB/3GvHicEQJyKp4H3jWzTWQu0bwL9SoLc71LAdJ2G1SlRTxX/3VKIPT3I"
            + "0myc4OLdaozeguDVBjMqGhoPaZWjbEktD9BeqvZchzmLYyWl+rpLkk1sUP8j5QWE"
            + "FSUE24M4RXu20i3Z4UwRSx+QJ6y0ajRihBg6lMs9w8ZFLsydJjUItYZRipoxKd6z"
            + "SdLyR7Y88PhDSy4BnPiayeE259qexyy0zy1w22xkXqGPQm9tmiTz2zMCAwEAAaNj"
            + "MGEwHwYDVR0jBBgwFoAUiBZ28+5dImSVCTC89MiGmBPOFWEwHQYDVR0OBBYEFIgW"
            + "dvPuXSJklQkwvPTIhpgTzhVhMA8GA1UdEwEB/wQFMAMBAf8wDgYDVR0PAQH/BAQD"
            + "AgEGMA0GCSqGSIb3DQEBBQUAA4IBAQBEJ1DSNURLiRja5xCMaiyV+sPE5oFKeoPu"
            + "MraofH2aMtbRyB+vnYT4T/OxpBOEFX6uQGQ2/5jLmQnRozgGlql7GlHyTRXCXK2y"
            + "sZ7tDAW33D6x5IU05Q6aD1fjez/oiwRyQdyAFXInPm/8V6nlJ+dGKe1ZnyFWM6yE"
            + "t3YgOe4R57Zu9njLGBy47k6MYUA/RPz1aN3qgB9jFu9fkWNF++YGcrbt4p/f9b9P"
            + "iGxQXkxuyq1cTALfsdqROwsmwKTcbOcJBccFNMBdo404Hn+orwEt4J/ijpPWEdca"
            + "TJORZBwI8AlFhL3vBirxCFP/N9M+K3360l9mPZmJuIBfxrYwJICwMIID4jCCAsqg"
            + "AwIBAgIRALpeBEAtiuOWnOU9EQEPV5wwDQYJKoZIhvcNAQEFBQAwEjEQMA4GA1UE"
            + "AwwHVGVzdCBDQTAeFw0xNTA5MTAxMDAwMDBaFw00OTEyMzExMTAwMDBaMB8xHTAb"
            + "BgNVBAMMFFRlc3QgSW50ZXJtZWRpYXRlIENBMIIBtzCCASwGByqGSM44BAEwggEf"
            + "AoGBALjHlfmpKj8BiEfekiLTnbYdZlo5Hz6E2dAjx+ryqv3jeGYbPTxh+pxrD0MI"
            + "IUKF+3o8Y+TBwBpbKnnZ/G2T/P6QXs8+l7H7Q4CUJKShdQ+PhpK8JXYaICN4VAtK"
            + "sP4PVhBMWLw/3VANh67JDwZz1Oa5soci3dAVQDWN8mc4PdbhAhUAoWrfRj14AUQT"
            + "759T/Men1dQ9o0ECgYEAgWPlEWkpvgfkCvyBMRiRWchS0suOUUL5RyqYKmVFpDE2"
            + "aKRMMFO5owlluJ1lm57f4zaddY8zAsT72tv0tTxz7nFAAPoX4QPOcSxYYapvEGRZ"
            + "klJRU4qrrXOPlXTia6jsWlgjnMaJ43zCBXteK2AdZ2DF7Yr9UPRuNukIzSYc4pcD"
            + "gYQAAoGAEH/cX4auYYjapwPvipulmUPLPB9GTPcZfcefLYH4FlAs/W1/vfer1kGZ"
            + "L/+urSu+5D/FonOGNE9VRnLhVO4SyOremfJTO0ZLA7w5ciQwcQRxwXX3vvYzxtiF"
            + "A2H7G7SHVcg8GDzyikHePQnyDwjgXf2C8dxcyasUA5FJb62YKo2jgZAwgY0wSAYD"
            + "VR0jBEEwP4AUiBZ28+5dImSVCTC89MiGmBPOFWGhFqQUMBIxEDAOBgNVBAMMB1Rl"
            + "c3QgQ0GCD155oq5k3ogYWMtf4v7cKzAdBgNVHQ4EFgQUbrrdokxqhLVQHnfZkpK1"
            + "PN7kUbowEgYDVR0TAQH/BAgwBgEB/wIBADAOBgNVHQ8BAf8EBAMCAQYwDQYJKoZI"
            + "hvcNAQEFBQADggEBALuttCfxVloX+cVZw97ny2C0Gx1gF2iQXPUq7QjsTs9xWB2X"
            + "2j9hMYS8x4oB8x2w59PRZpdtdlFvjxeLR9xrX1yFeqcGMZ2opEqxhtFhSE28Inv+"
            + "A+VWo/Je1T986XEgGMrIgPkW46Bc8xsNy63WHdcpj7U9xWU2Qcs8EPzTNV3ibevN"
            + "dCS6bwXEpgn2fSV7dsscaZGt9O43Co2iyGYqXZXqjRXOJkQmLFc/p5xPpsPhFXER"
            + "E65Ihdtl17XLiIt88mvOyfNl/X9c28lLf0+hlEqnmE1CDrvitKAyhHU7w1nXlQZZ"
            + "z6RWvilHqn0NkT3vTYhQBVXq5XiQwT8xLBjJJtwwggMyMIIC8KADAgECAhEAqb+H"
            + "N7UJjcW7Be4p3zp5eDALBgcqhkjOOAQDBQAwHzEdMBsGA1UEAwwUVGVzdCBJbnRl"
            + "cm1lZGlhdGUgQ0EwHhcNMTUwOTEwMTAwMDAwWhcNNDkxMjMxMTEwMDAwWjAaMRgw"
            + "FgYDVQQDDA9UZXN0IEVuZCBFbnRpdHkwggG3MIIBLAYHKoZIzjgEATCCAR8CgYEA"
            + "1D05rWP2xYCF1c3KrPwcv9sD953ZWsHDpM+SAzJXVSmLHnxFKM4cxA2uB6ZIaRvz"
            + "R1FwupUNhsjc5ucgmHzdnHrN5pOm9SiEYvJNrQXtRjXkuRsmnLPxYUlpXKQfzmfP"
            + "dmjox31nhf3nK8XX6H2pi5qSNZwdbfX6k3i7dwk2DkUCFQCP3zpxUb02BaBbMVVI"
            + "tsH2kQ9sVQKBgQCZCtkUfcxfkItf/m8O/HJN55E9kctZzSKTcbCsgkTG5Ws6S4Ui"
            + "enHRpVu4lPGsSdjZ+5z/4HKP8XnoMM7fX6o+7tnFtK+8Pfc46Tx4UR+f4eJrdCYD"
            + "50auFSqgyl8N/j8YGAmGfJC0XMx10eZgHRCdL3JfWfoM5aP8JJfQ++uUxgOBhAAC"
            + "gYAm+e5Obygj9FEja2Jke0+S9JBoDcU/g9GxXHrPWggaD04DGu1JL9qJBr4dYWDt"
            + "+MeUwnfRWHTKq3uKCPvSNosbIO+vcw8PKI777vYiIZSW5zrIeDqb1xDnd54m32Lj"
            + "gsl9M7DVLdgxTVJPtqWiRKclFMgKnlYHLSbWxzCF65OjQaOBsDCBrTBKBgNVHSME"
            + "QzBBgBRuut2iTGqEtVAed9mSkrU83uRRuqEWpBQwEjEQMA4GA1UEAwwHVGVzdCBD"
            + "QYIRALpeBEAtiuOWnOU9EQEPV5wwHQYDVR0OBBYEFJ0i7GBYsbjkyTTVEdohFS7Z"
            + "Fp0eMA4GA1UdDwEB/wQEAwIEkDATBgNVHSUEDDAKBggrBgEFBQcDBDAbBgNVHREE"
            + "FDASgRB0ZXN0QGV4YW1wbGUuY29tMAsGByqGSM44BAMFAAMvADAsAhR7rFQrGqy6"
            + "kvkxYop2xpjQWIZZWAIUcbcxbW2aBDoIKEh0D0rl8GNQuRMAADGCAQ4wggEKAgEB"
            + "MDQwHzEdMBsGA1UEAwwUVGVzdCBJbnRlcm1lZGlhdGUgQ0ECEQCpv4c3tQmNxbsF"
            + "7infOnl4MAkGBSsOAwIaBQCggYYwGAYJKoZIhvcNAQkDMQsGCSqGSIb3DQEHATAc"
            + "BgkqhkiG9w0BCQUxDxcNMTYwNjIxMDc1OTQ4WjAjBgkqhkiG9w0BCQQxFgQUL8YM"
            + "8wd3sUJTRPPu8y2DoqhOX1IwJwYJKoZIhvcNAQk0MRowGDAJBgUrDgMCGgUAoQsG"
            + "ByqGSM44BAMFADALBgcqhkjOOAQDBQAELjAsAhRabcKO78K8qs2+i5h2A1vfNRxn"
            + "FwIUffx3aUOGWzp0lKH4YU6am90kV5IAAAAAAAA=";

    private class ArrayBasedCertificateProvider implements CertificateProvider
    {
        Collection<X509CertifiedPublicKey> certs = new ArrayList<>();

        public ArrayBasedCertificateProvider(Collection<CertifiedPublicKey> certs)
        {
            for (CertifiedPublicKey cert : certs) {
                this.certs.add((X509CertifiedPublicKey) cert);
            }
        }

        @Override
        public CertifiedPublicKey getCertificate(byte[] keyIdentifier)
        {
            for (X509CertifiedPublicKey cert : certs) {
                if (Arrays.equals(cert.getSubjectKeyIdentifier(), keyIdentifier)) {
                    return cert;
                }
            }
            return null;
        }

        @Override
        public CertifiedPublicKey getCertificate(PrincipalIndentifier issuer, BigInteger serial)
        {
            for (X509CertifiedPublicKey cert : certs) {
                if (cert.getIssuer().equals(issuer) && cert.getSerialNumber().equals(serial)) {
                    return cert;
                }
            }
            return null;
        }

        @Override
        public CertifiedPublicKey getCertificate(PrincipalIndentifier issuer, BigInteger serial, byte[] keyIdentifier)
        {
            for (X509CertifiedPublicKey cert : certs) {
                if (cert.getIssuer().equals(issuer) && cert.getSerialNumber().equals(serial) && Arrays.equals(cert.getSubjectKeyIdentifier(), keyIdentifier)){
                    return cert;
                }
            }
            return null;
        }

        @Override
        public Collection<CertifiedPublicKey> getCertificate(PrincipalIndentifier subject)
        {
            List<CertifiedPublicKey> result = new ArrayList<>();
            for (X509CertifiedPublicKey cert : certs) {
                if (cert.getSubject().equals(subject)) {
                    result.add(cert);
                }
            }
            return result;
        }
    }

    @Inject
    @Named("Base64")
    private BinaryStringEncoder base64encoder;

    @Inject
    @Named("DSA")
    private AsymmetricKeyFactory dsaKeyFactory;

    @Inject
    @Named("X509")
    private CertificateFactory certFactory;

    @Inject
    private Converter<License> converter;

    @Inject
    @Named("DSAwithSHA1")
    private SignerFactory dsaSignerFactory;

    private SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");

    private CertifiedPublicKey v3CaCert;
    private CertifiedPublicKey v3InterCaCert;
    private CertifiedPublicKey v3Cert;
    private CertifiedKeyPair keyPair;
    private CertificateProvider certProvider;
    private SignedLicense signedLicense;


    @Override
    public void initialize() throws InitializationException
    {
        try {
            PrivateKeyParameters dsaPrivateKey = dsaKeyFactory.fromPKCS8(base64encoder.decode(DSA_PRIVATE_KEY));
            v3CaCert = certFactory.decode(base64encoder.decode(V3_CA_CERT));
            v3InterCaCert = certFactory.decode(base64encoder.decode(V3_ITERCA_CERT));
            v3Cert = certFactory.decode(base64encoder.decode(V3_CERT));
            keyPair = new CertifiedKeyPair(dsaPrivateKey, v3Cert);
            certProvider = new ArrayBasedCertificateProvider(Arrays.asList(v3CaCert, v3InterCaCert, v3Cert));
            signedLicense = converter.convert(License.class, base64encoder.decode(SIGNED_LICENSE));
        } catch(Exception e) {
            throw new InitializationException("Unable to initialized Signed License tests", e);
        }
    }

    @Override
    public CertifiedKeyPair getSigningKeyPair()
    {
        return keyPair;
    }

    @Override
    public SignerFactory getSignerFactory()
    {
        return dsaSignerFactory;
    }

    @Override
    public CertificateProvider getCertificateProvider()
    {
        return certProvider;
    }

    @Override
    public CertifiedPublicKey getRootCertificate()
    {
        return v3CaCert;
    }

    @Override
    public CertifiedPublicKey getIntermediateCertificate()
    {
        return v3InterCaCert;
    }

    @Override
    public CertifiedPublicKey getSigningCertificate()
    {
        return v3Cert;
    }

    @Override
    public SignedLicense getSignedLicense()
    {
        return signedLicense;
    }

    @Override
    public Converter<License> getLicenseConverter()
    {
        return converter;
    }
}
