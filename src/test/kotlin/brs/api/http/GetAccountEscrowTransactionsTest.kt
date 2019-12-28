package brs.api.http

import brs.entity.Account
import brs.entity.Escrow
import brs.entity.Escrow.Decision
import brs.entity.Escrow.DecisionType
import brs.common.AbstractUnitTest
import brs.common.QuickMocker
import brs.common.QuickMocker.MockParam
import brs.api.http.common.Parameters.ACCOUNT_PARAMETER
import brs.api.http.common.Parameters.ESCROWS_RESPONSE
import brs.api.http.common.ResultFields.AMOUNT_PLANCK_RESPONSE
import brs.api.http.common.ResultFields.DEADLINE_ACTION_RESPONSE
import brs.api.http.common.ResultFields.DEADLINE_RESPONSE
import brs.api.http.common.ResultFields.DECISION_RESPONSE
import brs.api.http.common.ResultFields.ID_RESPONSE
import brs.api.http.common.ResultFields.ID_RS_RESPONSE
import brs.api.http.common.ResultFields.RECIPIENT_RESPONSE
import brs.api.http.common.ResultFields.RECIPIENT_RS_RESPONSE
import brs.api.http.common.ResultFields.REQUIRED_SIGNERS_RESPONSE
import brs.api.http.common.ResultFields.SENDER_RESPONSE
import brs.api.http.common.ResultFields.SENDER_RS_RESPONSE
import brs.api.http.common.ResultFields.SIGNERS_RESPONSE
import brs.services.EscrowService
import brs.services.ParameterService
import brs.util.json.safeGetAsLong
import brs.util.json.safeGetAsString
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.mockk.mockk
import io.mockk.every
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class GetAccountEscrowTransactionsTest : AbstractUnitTest() {

    private lateinit var parameterServiceMock: ParameterService
    private lateinit var escrowServiceMock: EscrowService

    private lateinit var t: GetAccountEscrowTransactions

    @Before
    fun setUp() {
        parameterServiceMock = mockk(relaxed = true)
        escrowServiceMock = mockk(relaxed = true)

        t = GetAccountEscrowTransactions(parameterServiceMock, escrowServiceMock)
    }

    @Test
    fun processRequest() {
        val accountId: Long = 5

        val request = QuickMocker.httpServletRequest(
                MockParam(ACCOUNT_PARAMETER, accountId)
        )

        val account = mockk<Account>(relaxed = true)
        every { account.id } returns accountId
        every { parameterServiceMock.getAccount(eq(request)) } returns account

        val escrow = mockk<Escrow>(relaxed = true)
        every { escrow.id } returns 1L
        every { escrow.senderId } returns 2L
        every { escrow.recipientId } returns 3L
        every { escrow.amountPlanck } returns 4L
        every { escrow.requiredSigners } returns 5
        every { escrow.deadlineAction } returns DecisionType.UNDECIDED

        val decision = mockk<Decision>(relaxed = true)
        every { decision.accountId } returns 3L
        every { decision.decision } returns DecisionType.UNDECIDED

        val skippedDecision = mockk<Decision>(relaxed = true)
        every { skippedDecision.accountId } returns 5L

        val otherSkippedDecision = mockk<Decision>(relaxed = true)
        every { otherSkippedDecision.accountId } returns 6L

        every { escrow.recipientId } returns 5L
        every { escrow.senderId } returns 6L

        val decisionsIterator = mockCollection(decision, skippedDecision, otherSkippedDecision)
        every { escrow.decisions } returns decisionsIterator

        val escrowCollection = listOf(escrow)
        every { escrowServiceMock.getEscrowTransactionsByParticipant(eq(accountId)) } returns escrowCollection

        val resultOverview = t.processRequest(request) as JsonObject
        assertNotNull(resultOverview)

        val resultList = resultOverview.get(ESCROWS_RESPONSE) as JsonArray
        assertNotNull(resultList)
        assertEquals(1, resultList.size().toLong())

        val result = resultList.get(0) as JsonObject
        assertNotNull(result)

        assertEquals(escrow.id.toString(), result.get(ID_RESPONSE).safeGetAsString())
        assertEquals(escrow.senderId.toString(), result.get(SENDER_RESPONSE).safeGetAsString())
        assertEquals("BURST-2228-2222-BMNG-22222", result.get(SENDER_RS_RESPONSE).safeGetAsString())
        assertEquals(escrow.recipientId.toString(), result.get(RECIPIENT_RESPONSE).safeGetAsString())
        assertEquals("BURST-2227-2222-ZAYB-22222", result.get(RECIPIENT_RS_RESPONSE).safeGetAsString())
        assertEquals(escrow.amountPlanck.toString(), result.get(AMOUNT_PLANCK_RESPONSE).safeGetAsString())
        assertEquals(escrow.requiredSigners.toLong(), result.get(REQUIRED_SIGNERS_RESPONSE).safeGetAsLong())
        assertEquals(escrow.deadline.toLong(), result.get(DEADLINE_RESPONSE).safeGetAsLong())
        assertEquals("undecided", result.get(DEADLINE_ACTION_RESPONSE).safeGetAsString())

        val signersResult = result.get(SIGNERS_RESPONSE) as JsonArray
        assertEquals(1, signersResult.size().toLong())

        val signer = signersResult.get(0) as JsonObject
        assertEquals(decision.accountId!!.toString(), signer.get(ID_RESPONSE).safeGetAsString())
        assertEquals("BURST-2225-2222-QVC9-22222", signer.get(ID_RS_RESPONSE).safeGetAsString())
        assertEquals("undecided", signer.get(DECISION_RESPONSE).safeGetAsString())
    }
}
