/*
 *
 * Copyright (C) 2018 Taktik SA
 *
 * This file is part of FreeHealthConnector.
 *
 * FreeHealthConnector is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation.
 *
 * FreeHealthConnector is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with FreeHealthConnector.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.taktik.freehealth.middleware.service.impl

import be.cin.encrypted.BusinessContent
import be.cin.encrypted.EncryptedKnownContent
import be.fgov.ehealth.etee.crypto.utils.KeyManager
import be.fgov.ehealth.messageservices.core.v1.RequestType
import be.fgov.ehealth.messageservices.core.v1.SendTransactionRequest
import be.fgov.ehealth.messageservices.core.v1.SendTransactionResponse
import be.fgov.ehealth.mycarenet.attest.protocol.v1.SendAttestationRequest
import be.fgov.ehealth.mycarenet.commons.core.v3.CareProviderType
import be.fgov.ehealth.mycarenet.commons.core.v3.CareReceiverIdType
import be.fgov.ehealth.mycarenet.commons.core.v3.CommonInputType
import be.fgov.ehealth.mycarenet.commons.core.v3.IdType
import be.fgov.ehealth.mycarenet.commons.core.v3.LicenseType
import be.fgov.ehealth.mycarenet.commons.core.v3.NihiiType
import be.fgov.ehealth.mycarenet.commons.core.v3.OriginType
import be.fgov.ehealth.mycarenet.commons.core.v3.PackageType
import be.fgov.ehealth.mycarenet.commons.core.v3.RoutingType
import be.fgov.ehealth.mycarenet.commons.core.v3.ValueRefString
import be.fgov.ehealth.standards.kmehr.cd.v1.CDCONTENT
import be.fgov.ehealth.standards.kmehr.cd.v1.CDCONTENTschemes
import be.fgov.ehealth.standards.kmehr.cd.v1.CDCONTENTschemes.CD_NIHDI
import be.fgov.ehealth.standards.kmehr.cd.v1.CDCONTENTschemes.CD_NIHDI_RELATEDSERVICE
import be.fgov.ehealth.standards.kmehr.cd.v1.CDERRORMYCARENETschemes
import be.fgov.ehealth.standards.kmehr.cd.v1.CDHCPARTY
import be.fgov.ehealth.standards.kmehr.cd.v1.CDHCPARTYschemes
import be.fgov.ehealth.standards.kmehr.cd.v1.CDITEM
import be.fgov.ehealth.standards.kmehr.cd.v1.CDITEMschemes.CD_ITEM
import be.fgov.ehealth.standards.kmehr.cd.v1.CDITEMschemes.CD_ITEM_MYCARENET
import be.fgov.ehealth.standards.kmehr.cd.v1.CDSEX
import be.fgov.ehealth.standards.kmehr.cd.v1.CDSEXvalues
import be.fgov.ehealth.standards.kmehr.cd.v1.CDSTANDARD
import be.fgov.ehealth.standards.kmehr.cd.v1.CDTRANSACTION
import be.fgov.ehealth.standards.kmehr.cd.v1.CDTRANSACTIONschemes.CD_TRANSACTION_MYCARENET
import be.fgov.ehealth.standards.kmehr.cd.v1.CDUNIT
import be.fgov.ehealth.standards.kmehr.cd.v1.CDUNITschemes
import be.fgov.ehealth.standards.kmehr.dt.v1.TextType
import be.fgov.ehealth.standards.kmehr.id.v1.IDHCPARTY
import be.fgov.ehealth.standards.kmehr.id.v1.IDHCPARTYschemes
import be.fgov.ehealth.standards.kmehr.id.v1.IDKMEHR
import be.fgov.ehealth.standards.kmehr.id.v1.IDKMEHRschemes
import be.fgov.ehealth.standards.kmehr.id.v1.IDPATIENT
import be.fgov.ehealth.standards.kmehr.id.v1.IDPATIENTschemes
import be.fgov.ehealth.standards.kmehr.schema.v1.AuthorType
import be.fgov.ehealth.standards.kmehr.schema.v1.ContentType
import be.fgov.ehealth.standards.kmehr.schema.v1.CostType
import be.fgov.ehealth.standards.kmehr.schema.v1.FolderType
import be.fgov.ehealth.standards.kmehr.schema.v1.HcpartyType
import be.fgov.ehealth.standards.kmehr.schema.v1.HeaderType
import be.fgov.ehealth.standards.kmehr.schema.v1.ItemType
import be.fgov.ehealth.standards.kmehr.schema.v1.Kmehrmessage
import be.fgov.ehealth.standards.kmehr.schema.v1.PersonType
import be.fgov.ehealth.standards.kmehr.schema.v1.QuantityType
import be.fgov.ehealth.standards.kmehr.schema.v1.RecipientType
import be.fgov.ehealth.standards.kmehr.schema.v1.SenderType
import be.fgov.ehealth.standards.kmehr.schema.v1.SexType
import be.fgov.ehealth.standards.kmehr.schema.v1.StandardType
import be.fgov.ehealth.standards.kmehr.schema.v1.TransactionType
import be.fgov.ehealth.standards.kmehr.schema.v1.UnitType
import be.fgov.ehealth.technicalconnector.signature.AdvancedElectronicSignatureEnumeration
import be.fgov.ehealth.technicalconnector.signature.SignatureBuilderFactory
import be.fgov.ehealth.technicalconnector.signature.transformers.EncapsulationTransformer
import com.google.gson.Gson
import org.apache.commons.codec.binary.Base64
import org.apache.commons.lang.StringUtils
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.taktik.connector.business.mycarenet.attest.domain.AttestBuilderResponse
import org.taktik.connector.business.mycarenet.attest.domain.InputReference
import org.taktik.connector.business.mycarenet.attest.mappers.BlobMapper
import org.taktik.connector.business.mycarenetdomaincommons.builders.BlobBuilderFactory
import org.taktik.connector.business.mycarenetdomaincommons.util.McnConfigUtil
import org.taktik.connector.business.mycarenetdomaincommons.util.PropertyUtil
import org.taktik.connector.technical.config.ConfigFactory
import org.taktik.connector.technical.exception.TechnicalConnectorException
import org.taktik.connector.technical.exception.TechnicalConnectorExceptionValues
import org.taktik.connector.technical.idgenerator.IdGeneratorFactory
import org.taktik.connector.technical.service.etee.Crypto
import org.taktik.connector.technical.service.etee.CryptoFactory
import org.taktik.connector.technical.service.etee.domain.EncryptionToken
import org.taktik.connector.technical.service.keydepot.KeyDepotManagerFactory
import org.taktik.connector.technical.service.keydepot.impl.KeyDepotManagerImpl
import org.taktik.connector.technical.service.sts.security.Credential
import org.taktik.connector.technical.service.sts.security.impl.KeyStoreCredential
import org.taktik.connector.technical.utils.CertificateParser
import org.taktik.connector.technical.utils.ConnectorXmlUtils
import org.taktik.connector.technical.utils.IdentifierType
import org.taktik.connector.technical.utils.MarshallerHelper
import org.taktik.freehealth.middleware.dao.User
import org.taktik.freehealth.middleware.dto.mycarenet.MycarenetError
import org.taktik.freehealth.middleware.dto.eattest.EattestAcknowledgeType
import org.taktik.freehealth.middleware.dto.eattest.Eattest
import org.taktik.freehealth.middleware.dto.eattest.SendAttestResultWithResponse
import org.taktik.freehealth.middleware.service.EattestService
import org.taktik.freehealth.middleware.service.STSService
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.ByteArrayInputStream
import java.io.StringWriter
import java.math.BigDecimal
import java.util.*
import javax.xml.bind.JAXBContext
import javax.xml.namespace.NamespaceContext
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerException
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMResult
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

@Service
class EattestServiceImpl(private val stsService: STSService) : EattestService {
    private val log = LoggerFactory.getLogger(this.javaClass)
    private val config = ConfigFactory.getConfigValidator(listOf())
    private val freehealthEattestService: org.taktik.connector.business.eattest.EattestService = org.taktik.connector.business.eattest.impl.EattestServiceImpl()
    private val eAttestErrors =
        Gson().fromJson(
            this.javaClass.getResourceAsStream("/be/errors/eAttestErrors.json").reader(Charsets.UTF_8),
            arrayOf<MycarenetError>().javaClass
        ).associateBy({ it.uid }, { it })
    private val xPathFactory = XPathFactory.newInstance()

    fun NodeList.forEach(action: (Node) -> Unit) {
        (0 until this.length).asSequence().map { this.item(it) }.forEach { action(it) }
    }

    override fun sendAttest(
        keystoreId: UUID,
        tokenId: UUID,
        hcpNihii: String,
        hcpSsin: String,
        hcpFirstName: String,
        hcpLastName: String,
        hcpCbe: String,
        passPhrase: String,
        patientSsin: String,
        referenceDate: Int?,
        attest: Eattest
    ): SendAttestResultWithResponse? {
        val samlToken =
            stsService.getSAMLToken(tokenId, keystoreId, passPhrase)
                ?: throw IllegalArgumentException("Cannot obtain token for Ehealth Box operations")
        val keystore = stsService.getKeyStore(keystoreId, passPhrase)!!

        val credential = KeyStoreCredential(keystore, "authentication", passPhrase)
        val hokPrivateKeys = KeyManager.getDecryptionKeys(keystore, passPhrase.toCharArray())
        val crypto = CryptoFactory.getCrypto(credential, hokPrivateKeys)

        val detailId = "_" + IdGeneratorFactory.getIdGenerator("uuid").generateId()
        val inputReference = InputReference().inputReference

        val now = DateTime.now().withMillisOfSecond(0)
        val refDateTime = dateTime(referenceDate) ?: now
        val theDayBeforeRefDate = refDateTime.plusDays(-1)

        return extractEtk(credential)?.let {
            val sendTransactionRequest = SendTransactionRequest().apply {
                messageProtocoleSchemaVersion = BigDecimal("1.18")
                request = RequestType().apply {
                    id =
                        IDKMEHR().apply {
                            s = IDKMEHRschemes.ID_KMEHR; sv = "1.0"; value = hcpNihii + "." +
                            refDateTime.toString("yyyyMMddHHmmss")
                        }
                    author = AuthorType().apply {
                        hcparties.add(HcpartyType().apply {
                            ids.add(IDHCPARTY().apply { s = IDHCPARTYschemes.ID_HCPARTY; sv = "1.0"; value = hcpNihii })
                            ids.add(IDHCPARTY().apply { s = IDHCPARTYschemes.INSS; sv = "1.0"; value = hcpSsin })
                            cds.add(CDHCPARTY().apply {
                                s = CDHCPARTYschemes.CD_HCPARTY; sv = "1.10"; value =
                                "persphysician"
                            })
                            firstname = hcpFirstName
                            familyname = hcpLastName
                        })
                    }
                    date = now; time = now
                }

                kmehrmessage = Kmehrmessage().apply {
                    header = HeaderType().apply {
                        standard =
                            StandardType().apply {
                                cd =
                                    CDSTANDARD().apply { s = "CD-STANDARD"; sv = "1.19"; value = "20160901" }
                            }
                        ids.add(IDKMEHR().apply { s = IDKMEHRschemes.ID_KMEHR; sv = "1.0"; value = "1" })
                        date = refDateTime; time = refDateTime
                        sender = SenderType().apply {
                            hcparties.add(HcpartyType().apply {
                                ids.add(IDHCPARTY().apply {
                                    s = IDHCPARTYschemes.ID_HCPARTY; sv = "1.0"; value =
                                    hcpNihii
                                })
                                ids.add(IDHCPARTY().apply { s = IDHCPARTYschemes.INSS; sv = "1.0"; value = hcpSsin })
                                cds.add(CDHCPARTY().apply {
                                    s = CDHCPARTYschemes.CD_HCPARTY; sv = "1.10"; value =
                                    "persphysician"
                                })
                                firstname = hcpFirstName
                                familyname = hcpLastName
                            })
                        }
                        recipients.add(RecipientType().apply {
                            hcparties.add(HcpartyType().apply {
                                cds.add(CDHCPARTY().apply {
                                    s = CDHCPARTYschemes.CD_HCPARTY; sv = "1.10"; value =
                                    "application"
                                })
                                name = "mycarenet"
                            })
                        })
                    }
                    folders.add(FolderType().apply {
                        var trnsId = 1

                        ids.add(IDKMEHR().apply { s = IDKMEHRschemes.ID_KMEHR; sv = "1.0"; value = "1" })
                        patient = PersonType().apply {
                            ids.add(IDPATIENT().apply {
                                s = IDPATIENTschemes.ID_PATIENT; sv = "1.0"; value =
                                patientSsin
                            })
                            firstnames.add("")
                            familyname = ""
                            sex =
                                SexType().apply {
                                    cd =
                                        CDSEX().apply { s = "CD-SEX"; sv = "1.1"; value = CDSEXvalues.UNKNOWN }
                                }
                        }
                        transactions.addAll(listOf(TransactionType().apply {
                            var itemId = 1

                            ids.add(IDKMEHR().apply {
                                s = IDKMEHRschemes.ID_KMEHR; sv = "1.0"; value =
                                (trnsId++).toString()
                            })
                            cds.add(CDTRANSACTION().apply { s = CD_TRANSACTION_MYCARENET; sv = "1.2"; value = "cga" })
                            date = refDateTime; time = refDateTime
                            author = AuthorType().apply {
                                hcparties.add(HcpartyType().apply {
                                    ids.add(IDHCPARTY().apply {
                                        s = IDHCPARTYschemes.ID_HCPARTY; sv = "1.0"; value =
                                        hcpNihii
                                    })
                                    ids.add(IDHCPARTY().apply {
                                        s = IDHCPARTYschemes.INSS; sv = "1.0"; value =
                                        hcpSsin
                                    })
                                    cds.add(CDHCPARTY().apply {
                                        s = CDHCPARTYschemes.CD_HCPARTY; sv = "1.10"; value =
                                        "persphysician"
                                    })
                                    firstname = hcpFirstName
                                    familyname = hcpLastName
                                })
                            }
                            isIscomplete = true
                            isIsvalidated = true
                            item.addAll(listOf(ItemType().apply {
                                ids.add(IDKMEHR().apply {
                                    s = IDKMEHRschemes.ID_KMEHR; sv = "1.0"; value =
                                    (itemId++).toString()
                                })
                                cds.add(CDITEM().apply {
                                    s = CD_ITEM_MYCARENET; sv = "1.3"; value =
                                    "patientpaid"
                                })
                                cost = CostType().apply {
                                    decimal =
                                        BigDecimal.valueOf(attest.codes.sumBy {
                                            Math.round(
                                                ((it.reimbursement ?: 0.0) + (it.reglementarySupplement ?: 0.0)) * 100
                                            ).toInt()
                                        }.toLong()).divide(BigDecimal(100L))
                                    unit = UnitType().apply {
                                        cd =
                                            CDUNIT().apply {
                                                s = CDUNITschemes.CD_CURRENCY; sv = "1.0"; value =
                                                "EUR"
                                            }
                                    }
                                }
                            },
                                               attest.codes.sumBy {
                                                   Math.round((it.doctorSupplement ?: 0.0) * 100)
                                                       .toInt()
                                               }.let {
                                                   if (it > 0) ItemType().apply {
                                                       ids.add(IDKMEHR().apply {
                                                           s = IDKMEHRschemes.ID_KMEHR; sv = "1.0"; value =
                                                           (itemId++).toString()
                                                       })
                                                       cds.add(CDITEM().apply {
                                                           s = CD_ITEM_MYCARENET; sv = "1.3"; value =
                                                           "supplement"
                                                       })
                                                       cost = CostType().apply {
                                                           decimal =
                                                               BigDecimal.valueOf(it.toLong()).divide(BigDecimal("100"))
                                                           unit = UnitType().apply {
                                                               cd =
                                                                   CDUNIT().apply {
                                                                       s = CDUNITschemes.CD_CURRENCY; sv =
                                                                       "1.0"; value = "EUR"
                                                                   }
                                                           }
                                                       }
                                                   } else null
                                               },
                                               ItemType().apply {
                                                   ids.add(IDKMEHR().apply {
                                                       s = IDKMEHRschemes.ID_KMEHR; sv = "1.0"; value =
                                                       (itemId++).toString()
                                                   })
                                                   cds.add(CDITEM().apply {
                                                       s = CD_ITEM_MYCARENET; sv = "1.3"; value =
                                                       "paymentreceivingparty"
                                                   })
                                                   contents.add(ContentType().apply {
                                                       ids.add(IDKMEHR().apply {
                                                           s =
                                                               IDKMEHRschemes.ID_CBE; sv = "1.0"; value = hcpCbe
                                                       })
                                                   })
                                               }).filterNotNull()
                            )
                        }).plus(attest.codes.map { code ->
                            TransactionType().apply {
                                ids.add(IDKMEHR().apply {
                                    s = IDKMEHRschemes.ID_KMEHR; sv = "1.0"; value =
                                    (trnsId++).toString()
                                })
                                cds.add(CDTRANSACTION().apply {
                                    s = CD_TRANSACTION_MYCARENET; sv = "1.2"; value =
                                    "cgd"
                                })
                                date = refDateTime; time = refDateTime
                                author = AuthorType().apply {
                                    hcparties.add(HcpartyType().apply {
                                        ids.add(IDHCPARTY().apply {
                                            s = IDHCPARTYschemes.ID_HCPARTY; sv = "1.0"; value =
                                            hcpNihii
                                        })
                                        ids.add(IDHCPARTY().apply {
                                            s = IDHCPARTYschemes.INSS; sv = "1.0"; value =
                                            hcpSsin
                                        })
                                        cds.add(CDHCPARTY().apply {
                                            s = CDHCPARTYschemes.CD_HCPARTY; sv =
                                            "1.10"; value = "persphysician"
                                        })
                                        firstname = hcpFirstName
                                        familyname = hcpLastName
                                    })
                                }
                                isIscomplete = true
                                isIsvalidated = true

                                var itemId = 1

                                item.addAll(listOf(ItemType().apply {
                                    ids.add(IDKMEHR().apply {
                                        s = IDKMEHRschemes.ID_KMEHR; sv = "1.0"; value =
                                        (itemId++).toString()
                                    })
                                    cds.add(CDITEM().apply { s = CD_ITEM; sv = "1.10"; value = "claim" })
                                    contents.addAll(listOf(ContentType().apply {
                                        cds.add(CDCONTENT().apply {
                                            s = CD_NIHDI; sv =
                                            "1.0"; value = code.riziv
                                        })
                                    },
                                                           ContentType().apply {
                                                               cds.add(CDCONTENT().apply {
                                                                   s =
                                                                       CDCONTENTschemes.LOCAL; sv = "1.0"; sl =
                                                                   "NIHDI-CLAIM-NORM"; value = code.norm.toString()
                                                               })
                                                           },
                                                           code.relativeService?.let {
                                                               ContentType().apply {
                                                                   cds.add(CDCONTENT().apply {
                                                                       s =
                                                                           CD_NIHDI_RELATEDSERVICE; sv = "1.0"; value =
                                                                       code.relativeService
                                                                   })
                                                               }
                                                           }).filterNotNull())
                                    quantity = QuantityType().apply { decimal = BigDecimal(code.quantity) }
                                }, ItemType().apply {
                                    ids.add(IDKMEHR().apply {
                                        s = IDKMEHRschemes.ID_KMEHR; sv = "1.0"; value =
                                        (itemId++).toString()
                                    })
                                    cds.add(CDITEM().apply { s = CD_ITEM; sv = "1.10"; value = "encounterdatetime" })
                                    contents.add(ContentType().apply { date = dateTime(code.date) ?: refDateTime })
                                }, code.location?.let { loc ->
                                    ItemType().apply {
                                        ids.add(IDKMEHR().apply {
                                            s = IDKMEHRschemes.ID_KMEHR; sv = "1.0"; value =
                                            (itemId++).toString()
                                        })
                                        cds.add(CDITEM().apply {
                                            s = CD_ITEM; sv = "1.10"; value =
                                            "encounterlocation"
                                        })
                                        contents.addAll(listOf(ContentType().apply {
                                            hcparty = HcpartyType().apply {
                                                ids.add(IDHCPARTY().apply {
                                                    s =
                                                        IDHCPARTYschemes.ID_HCPARTY; sv = "1.0"; value =
                                                    loc.idHcParty
                                                })
                                                cds.add(CDHCPARTY().apply {
                                                    s =
                                                        CDHCPARTYschemes.CD_HCPARTY; sv = "1.10"; value =
                                                    loc.cdHcParty
                                                })
                                            }
                                        }))
                                    }
                                }, code.requestor?.let { req ->
                                    ItemType().apply {
                                        ids.add(IDKMEHR().apply {
                                            s = IDKMEHRschemes.ID_KMEHR; sv = "1.0"; value =
                                            (itemId++).toString()
                                        })
                                        cds.add(CDITEM().apply { s = CD_ITEM; sv = "1.10"; value = "requestor" })
                                        contents.addAll(listOf(ContentType().apply {
                                            hcparty = HcpartyType().apply {
                                                ids.add(IDHCPARTY().apply {
                                                    s =
                                                        IDHCPARTYschemes.ID_HCPARTY; sv = "1.0"; value =
                                                    req.hcp!!.idHcParty
                                                })
                                                req.hcp!!.idSsin?.let { ssin ->
                                                    ids.add(IDHCPARTY().apply {
                                                        s =
                                                            IDHCPARTYschemes.INSS; sv = "1.0"; value = ssin
                                                    })
                                                }
                                                cds.add(CDHCPARTY().apply {
                                                    s =
                                                        CDHCPARTYschemes.CD_HCPARTY; sv = "1.10"; value =
                                                    req.hcp!!.cdHcParty
                                                })
                                                firstname = req.hcp!!.firstName ?: ""
                                                familyname = req.hcp!!.lastName ?: ""
                                            }
                                        },
                                                               ContentType().apply {
                                                                   date = dateTime(req.date)
                                                                       ?: theDayBeforeRefDate
                                                               }))
                                    }
                                }, code.gmdManager?.let { gmdm ->
                                    ItemType().apply {
                                        ids.add(IDKMEHR().apply {
                                            s = IDKMEHRschemes.ID_KMEHR; sv = "1.0"; value =
                                            (itemId++).toString()
                                        })
                                        cds.add(CDITEM().apply { s = CD_ITEM; sv = "1.10"; value = "gmdmanager" })
                                        contents.addAll(listOf(ContentType().apply {
                                            hcparty = HcpartyType().apply {
                                                ids.add(IDHCPARTY().apply {
                                                    s =
                                                        IDHCPARTYschemes.ID_HCPARTY; sv = "1.0"; value =
                                                    gmdm.idHcParty
                                                })
                                                gmdm.idSsin?.let { ssin ->
                                                    ids.add(IDHCPARTY().apply {
                                                        s =
                                                            IDHCPARTYschemes.INSS; sv = "1.0"; value = ssin
                                                    })
                                                }
                                                cds.add(CDHCPARTY().apply {
                                                    s =
                                                        CDHCPARTYschemes.CD_HCPARTY; sv = "1.10"; value =
                                                    gmdm.cdHcParty
                                                })
                                                firstname = gmdm.firstName ?: ""
                                                familyname = gmdm.lastName ?: ""
                                            }
                                        }))
                                    }
                                }, code.internship?.let { intern ->
                                    ItemType().apply {
                                        ids.add(IDKMEHR().apply {
                                            s = IDKMEHRschemes.ID_KMEHR; sv = "1.0"; value =
                                            (itemId++).toString()
                                        })
                                        cds.add(CDITEM().apply {
                                            s = CD_ITEM_MYCARENET; sv = "1.3"; value =
                                            "internship"
                                        })
                                        contents.addAll(listOf(ContentType().apply {
                                            hcparty = HcpartyType().apply {
                                                ids.add(IDHCPARTY().apply {
                                                    s =
                                                        IDHCPARTYschemes.ID_HCPARTY; sv = "1.0"; value =
                                                    intern.idHcParty
                                                })
                                                intern.idSsin?.let { ssin ->
                                                    ids.add(IDHCPARTY().apply {
                                                        s =
                                                            IDHCPARTYschemes.INSS; sv = "1.0"; value = ssin
                                                    })
                                                }
                                                cds.add(CDHCPARTY().apply {
                                                    s =
                                                        CDHCPARTYschemes.CD_HCPARTY; sv = "1.10"; value =
                                                    intern.cdHcParty
                                                })
                                                firstname = intern.firstName ?: ""
                                                familyname = intern.lastName ?: ""
                                            }
                                        }))
                                    }
                                }, code.cardReading?.let { cr ->
                                    ItemType().apply {
                                        ids.add(IDKMEHR().apply {
                                            s = IDKMEHRschemes.ID_KMEHR; sv = "1.0"; value =
                                            (itemId++).toString()
                                        })
                                        cds.add(CDITEM().apply { s = CD_ITEM; sv = "1.10"; value = "documentidentity" })
                                        contents.addAll(listOf(ContentType().apply {
                                            date = (dateTime(cr.date) ?: now)
                                            time =
                                                cr.time?.let {
                                                    dateTime(cr.date)?.withHourOfDay(it / 10000)
                                                        ?.withMinuteOfHour((it / 100) % 100)
                                                        ?.withSecondOfMinute(it % 100)
                                                }
                                        },
                                                               ContentType().apply {
                                                                   cds.add(CDCONTENT().apply {
                                                                       s =
                                                                           CDCONTENTschemes.LOCAL; sv = "1.0"; sl =
                                                                       "NIHDI-ID-DOC-INPUT-TYPE"; value =
                                                                       cr.inputType.toString()
                                                                   })
                                                               },
                                                               cr.manualInputReason?.let {
                                                                   ContentType().apply {
                                                                       cds.add(CDCONTENT().apply {
                                                                           s =
                                                                               CDCONTENTschemes.LOCAL; sv = "1.0"; sl =
                                                                           "NIHDI-ID-DOC-MANUAL-INOUT-JUSTIFICATION"; value =
                                                                           it.toString()
                                                                       })
                                                                   }
                                                               },
                                                               ContentType().apply {
                                                                   cds.add(CDCONTENT().apply {
                                                                       s =
                                                                           CDCONTENTschemes.LOCAL; sv = "1.0"; sl =
                                                                       "NIHDI-ID-DOC-MEDIA-TYPE"; value =
                                                                       cr.mediaType.toString()
                                                                   })
                                                               },
                                                               cr.serial?.let {
                                                                   ContentType().apply {
                                                                       texts.add(TextType().apply {
                                                                           l =
                                                                               "en"; value = it
                                                                       })
                                                                   }
                                                               }).filterNotNull())
                                    }
                                }).filterNotNull())
                            }
                        }))
                    })
                }
            }
            val kmehrMarshallHelper =
            MarshallerHelper(SendTransactionRequest::class.java, SendTransactionRequest::class.java)
            val requestXml = kmehrMarshallHelper.toXMLByteArray(sendTransactionRequest)

            try{
                freehealthEattestService.sendAttestion(samlToken, SendAttestationRequest().apply {
                    val encryptedKnownContent = EncryptedKnownContent()
                    encryptedKnownContent.replyToEtk = it.encoded
                    val businessContent = BusinessContent().apply { id = detailId }
                    encryptedKnownContent.businessContent = businessContent

                    businessContent.value = requestXml
                    log.info("Request is: " + businessContent.value.toString(Charsets.UTF_8))
                    val xmlByteArray = handleEncryption(encryptedKnownContent, credential, crypto, detailId)
                    val blob =
                        BlobBuilderFactory.getBlobBuilder("attest")
                            .build(
                                xmlByteArray,
                                "none",
                                detailId,
                                "text/xml",
                                null as String?,
                                "encryptedForKnownCINNIC"
                            )
                    blob.messageName = "E-ATTEST"

                    val principal = SecurityContextHolder.getContext().authentication?.principal as? User
                    val packageInfo = McnConfigUtil.retrievePackageInfo("attest", principal?.mcnLicense, principal?.mcnPassword)

                    this.commonInput = CommonInputType().apply {
                        request =
                            be.fgov.ehealth.mycarenet.commons.core.v3.RequestType()
                                .apply { isIsTest = config.getProperty("endpoint.genins")?.contains("-acpt") ?: false }
                        this.inputReference = inputReference
                        origin = OriginType().apply {
                            `package` = PackageType().apply {
                                license = LicenseType().apply {
                                    username = packageInfo.userName
                                    password = packageInfo.password
                                }
                                name = ValueRefString().apply { value = packageInfo.packageName }
                            }
                            siteID =
                                ValueRefString().apply {
                                    value =
                                        config.getProperty(
                                            "mycarenet.${PropertyUtil.retrieveProjectNameToUse(
                                                "genins",
                                                "mycarenet."
                                            )}.site.id"
                                        )
                                }
                            careProvider = CareProviderType().apply {
                                nihii =
                                    NihiiType().apply {
                                        quality = "doctor"; value =
                                        ValueRefString().apply { value = hcpNihii }
                                    }
                                physicalPerson = IdType().apply {
                                    name = ValueRefString().apply { value = hcpFirstName + " " + hcpLastName }
                                    ssin = ValueRefString().apply { value = hcpSsin }
                                    nihii =
                                        NihiiType().apply {
                                            quality = "doctor"; value =
                                            ValueRefString().apply { value = hcpNihii }
                                        }
                                }
                            }
                        }
                    }
                    this.id = IdGeneratorFactory.getIdGenerator("xsid").generateId()
                    this.issueInstant = DateTime()
                    this.routing = RoutingType().apply {
                        careReceiver = CareReceiverIdType().apply {
                            ssin = patientSsin
                        }
                        this.referenceDate = refDateTime
                    }
                    this.detail = BlobMapper.mapBlobTypefromBlob(blob)
                })
            }catch (e:Exception){
                print(e.toString())
            }
            val sendAttestationResponse =
                freehealthEattestService.sendAttestion(samlToken, SendAttestationRequest().apply {
                    val encryptedKnownContent = EncryptedKnownContent()
                    encryptedKnownContent.replyToEtk = it.encoded
                    val businessContent = BusinessContent().apply { id = detailId }
                    encryptedKnownContent.businessContent = businessContent

                    businessContent.value = requestXml
                    log.info("Request is: " + businessContent.value.toString(Charsets.UTF_8))
                    val xmlByteArray = handleEncryption(encryptedKnownContent, credential, crypto, detailId)
                    val blob =
                        BlobBuilderFactory.getBlobBuilder("attest")
                            .build(
                                xmlByteArray,
                                "none",
                                detailId,
                                "text/xml",
                                null as String?,
                                "encryptedForKnownCINNIC"
                            )
                    blob.messageName = "E-ATTEST"

                    val principal = SecurityContextHolder.getContext().authentication?.principal as? User
                    val packageInfo = McnConfigUtil.retrievePackageInfo("attest", principal?.mcnLicense, principal?.mcnPassword)

                    this.commonInput = CommonInputType().apply {
                        request =
                            be.fgov.ehealth.mycarenet.commons.core.v3.RequestType()
                                .apply { isIsTest = config.getProperty("endpoint.genins")?.contains("-acpt") ?: false }
                        this.inputReference = inputReference
                        origin = OriginType().apply {
                            `package` = PackageType().apply {
                                license = LicenseType().apply {
                                    username = packageInfo.userName
                                    password = packageInfo.password
                                }
                                name = ValueRefString().apply { value = packageInfo.packageName }
                            }
                            siteID =
                                ValueRefString().apply {
                                    value =
                                        config.getProperty(
                                            "mycarenet.${PropertyUtil.retrieveProjectNameToUse(
                                                "genins",
                                                "mycarenet."
                                            )}.site.id"
                                        )
                                }
                            careProvider = CareProviderType().apply {
                                nihii =
                                    NihiiType().apply {
                                        quality = "doctor"; value =
                                        ValueRefString().apply { value = hcpNihii }
                                    }
                                physicalPerson = IdType().apply {
                                    name = ValueRefString().apply { value = hcpFirstName + " " + hcpLastName }
                                    ssin = ValueRefString().apply { value = hcpSsin }
                                    nihii =
                                        NihiiType().apply {
                                            quality = "doctor"; value =
                                            ValueRefString().apply { value = hcpNihii }
                                        }
                                }
                            }
                        }
                    }
                    this.id = IdGeneratorFactory.getIdGenerator("xsid").generateId()
                    this.issueInstant = DateTime()
                    this.routing = RoutingType().apply {
                        careReceiver = CareReceiverIdType().apply {
                            ssin = patientSsin
                        }
                        this.referenceDate = refDateTime
                    }
                    try{
                        BlobMapper.mapBlobTypefromBlob(blob)
                    }catch(e:Exception){
                        print(e.toString())
                    }
                    this.detail = BlobMapper.mapBlobTypefromBlob(blob)
                })
            val blobType = sendAttestationResponse.`return`.detail
            val blob = BlobMapper.mapBlobfromBlobType(blobType)
            val unsealedData =
                crypto.unseal(Crypto.SigningPolicySelector.WITHOUT_NON_REPUDIATION, blob.content).contentAsByte
            val encryptedKnownContent =
                MarshallerHelper(EncryptedKnownContent::class.java, EncryptedKnownContent::class.java).toObject(
                    unsealedData
                )
            val xades = encryptedKnownContent!!.xades
            val builder = SignatureBuilderFactory.getSignatureBuilder(AdvancedElectronicSignatureEnumeration.XAdES)
            val options = emptyMap<String, Any>()
            val signatureVerificationResult = xades?.let { builder.verify(unsealedData, it, options) }

            val decryptedAndVerifiedResponse =
                AttestBuilderResponse(
                    MarshallerHelper(
                        SendTransactionResponse::class.java,
                        SendTransactionResponse::class.java
                    ).toObject(encryptedKnownContent.businessContent.value), signatureVerificationResult
                )

            val errors = decryptedAndVerifiedResponse.sendTransactionResponse.acknowledge.errors?.flatMap { e ->
                e.cds.find { it.s == CDERRORMYCARENETschemes.CD_ERROR }?.value?.let { ec ->
                    extractError(requestXml, ec, e.url)
                } ?: setOf()
            }
            decryptedAndVerifiedResponse.sendTransactionResponse?.kmehrmessage?.folders?.firstOrNull()?.let { folder ->
                SendAttestResultWithResponse(EattestAcknowledgeType(
                    iscomplete = decryptedAndVerifiedResponse.sendTransactionResponse.acknowledge.isIscomplete,
                    errors = errors ?: listOf()
                ),
                 invoicingNumber = folder.transactions.find { it.cds.any { it.s == CD_TRANSACTION_MYCARENET && it.value == "cga" } }?.let {
                     it.item.find { it.cds.any { it.s == CD_ITEM_MYCARENET && it.value == "invoicingnumber" } }
                         ?.contents?.firstOrNull()?.texts?.firstOrNull()?.value
                 },
                 attest = Eattest(codes = folder.transactions?.filter { it.cds.any { it.s == CD_TRANSACTION_MYCARENET && it.value == "cgd" } }?.map { t ->
                     Eattest.EattestCode(
                         riziv = t.item.find { it.cds.any { it.s == CD_ITEM && it.value == "claim" } }?.contents?.mapNotNull { it.cds?.find { it.s == CD_NIHDI }?.value }?.firstOrNull(),
                         fee = t.item.find { it.cds.any { it.s == CD_ITEM_MYCARENET && it.value == "fee" } }?.cost?.decimal?.toDouble()
                     )
                 } ?: listOf()),
                 kmehrMessage = encryptedKnownContent.businessContent.value,
                 xades = xades,
                 xmlRequest = requestXml.toString(Charsets.UTF_8)
                );
            } ?: SendAttestResultWithResponse(EattestAcknowledgeType(
                    iscomplete = decryptedAndVerifiedResponse.sendTransactionResponse.acknowledge.isIscomplete,
                    errors = errors ?: listOf()
                ),
                kmehrMessage = null,
                xades = xades,
                xmlRequest = requestXml.toString(Charsets.UTF_8)
            )
        }
    }

    private fun dateTime(intDate: Int?) = intDate?.let {
        DateTime(0).withYear(intDate / 10000).withMonthOfYear((intDate / 100) % 100)
            .withDayOfMonth(intDate % 100)
    }

    private fun extractError(sendTransactionRequest: ByteArray, ec: String, errorUrl: String?): Set<MycarenetError> {
        return errorUrl?.let { url ->
            val factory = DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = true
            val builder = factory.newDocumentBuilder()

            val xpath = xPathFactory.newXPath()
            val expr = xpath.compile(if (url.startsWith("/")) url else "/" + url)
            val result = mutableSetOf<MycarenetError>()

            (expr.evaluate(
                builder.parse(ByteArrayInputStream(sendTransactionRequest)),
                XPathConstants.NODESET
            ) as NodeList).let { it ->
                if (it.length > 0) {
                    var node = it.item(0)
                    val textContent = node.textContent
                    var base = "/" + nodeDescr(node)
                    while (node.parentNode != null && node.parentNode is Element) {
                        base = "/${nodeDescr(node.parentNode)}$base"
                        node = node.parentNode
                    }
                    val elements =
                        eAttestErrors.values.filter {
                            it.path == base && it.code == ec && (it.regex == null || url.matches(Regex(".*" + it.regex + ".*")))
                        }
                    elements.forEach { it.value = textContent }
                    result.addAll(elements)
                } else {
                    result.add(
                        MycarenetError(
                            code = ec,
                            path = url,
                            msgFr = "Erreur générique, xpath invalide",
                            msgNl = "Onbekend foutmelding, xpath ongeldig"
                                                                                     )
                    )
                }
            }
            result
        } ?: setOf()
    }

    private fun nodeDescr(node: Node): String {
        val localName = node.localName ?: node.nodeName?.replace(Regex(".+?:(.+)"), "$1") ?: "unknown"
        val xpath = xPathFactory.newXPath()
        xpath.namespaceContext = object : NamespaceContext {
            override fun getNamespaceURI(prefix: String?) = "http://www.ehealth.fgov.be/standards/kmehr/schema/v1"
            override fun getPrefix(namespaceURI: String?) = "ns1"
            override fun getPrefixes(namespaceURI: String?): Iterator<Any?> =
                if (namespaceURI == "http://www.ehealth.fgov.be/standards/kmehr/schema/v1") listOf("ns1").iterator() else listOf<String>().iterator()
        }
        if (localName == "transaction") {
            return "transaction[${xpath.evaluate("ns1:cd[@S=\"CD-TRANSACTION-MYCARENET\"]", node)}]"
        }
        if (localName == "item") {
            return "item[${xpath.evaluate("ns1:cd[@S=\"CD-ITEM-MYCARENET\" or @S=\"CD-ITEM\"]", node)}]"
        }
        if (localName == "cd" && node is Element) {
            return "cd[${node.getAttribute("S") ?: node.getAttribute("SL")}]"
        }
        return localName
    }

    private fun handleEncryption(
        request: EncryptedKnownContent,
        credential: Credential,
        crypto: Crypto,
        detailId: String
    ): ByteArray? {
        val marshaller = JAXBContext.newInstance(request.javaClass).createMarshaller()
        val res = DOMResult()
        marshaller.marshal(request, res)

        val doc = res.node as Document

        val nodes = doc.getElementsByTagNameNS("urn:be:cin:encrypted", "EncryptedKnownContent")
        val content = toStringOmittingXmlDeclaration(nodes)
        val builder = SignatureBuilderFactory.getSignatureBuilder(AdvancedElectronicSignatureEnumeration.XAdES)
        val options = HashMap<String, Any>()
        val tranforms = ArrayList<String>()
        tranforms.add("http://www.w3.org/2000/09/xmldsig#base64")
        tranforms.add("http://www.w3.org/2001/10/xml-exc-c14n#")
        options.put("transformerList", tranforms)
        options.put("baseURI", detailId)
        options.put("encapsulate", true)
        options.put("encapsulate-transformer", EncapsulationTransformer { signature ->
            val result = signature.ownerDocument.createElementNS("urn:be:cin:encrypted", "Xades")
            result.textContent = Base64.encodeBase64String(ConnectorXmlUtils.toByteArray(signature))
            result
        })
        val encryptedKnowContent = builder.sign(credential, content.toByteArray(charset("UTF-8")), options)
        return crypto.seal(
            Crypto.SigningPolicySelector.WITH_NON_REPUDIATION,
            KeyDepotManagerFactory.getKeyDepotManager().getEtkSet(
                IdentifierType.CBE,
                820563481L,
                "MYCARENET"
            ),
            encryptedKnowContent
        )
    }

    @Throws(TransformerException::class)
    private fun toStringOmittingXmlDeclaration(nodes: NodeList): String {
        val sb = StringBuilder()
        val tf = TransformerFactory.newInstance()
        val serializer = tf.newTransformer()
        serializer.setOutputProperty("omit-xml-declaration", "yes")

        for (i in 0 until nodes.length) {
            val sw = StringWriter()
            serializer.transform(DOMSource(nodes.item(i)), StreamResult(sw))
            sb.append(sw.toString())
        }

        return sb.toString()
    }

    private fun extractEtk(cred: KeyStoreCredential): EncryptionToken? {
        val parser = CertificateParser(cred.certificate)
        if (parser.identifier != null && !StringUtils.isEmpty(parser.id) && StringUtils.isNumeric(parser.id)) {
            try {
                return KeyDepotManagerImpl.getInstance()
                    .getEtk(parser.identifier, java.lang.Long.parseLong(parser.id), parser.application)
            } catch (ex: NumberFormatException) {
                log.error(TechnicalConnectorExceptionValues.ERROR_ETK_NOTFOUND.message)
                throw TechnicalConnectorException(TechnicalConnectorExceptionValues.ERROR_ETK_NOTFOUND, ex)
            }
        } else {
            log.error(TechnicalConnectorExceptionValues.ERROR_ETK_NOTFOUND.message)
            throw TechnicalConnectorException(TechnicalConnectorExceptionValues.ERROR_ETK_NOTFOUND, *arrayOfNulls(0))
        }
    }
}
