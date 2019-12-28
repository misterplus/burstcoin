package brs.api.http

import brs.entity.Asset
import brs.services.AssetExchangeService
import brs.common.AbstractUnitTest
import brs.common.QuickMocker
import brs.common.QuickMocker.MockParam
import brs.api.http.common.Parameters.FIRST_INDEX_PARAMETER
import brs.api.http.common.Parameters.LAST_INDEX_PARAMETER
import brs.api.http.common.ResultFields.ASSETS_RESPONSE
import brs.api.http.common.ResultFields.ASSET_RESPONSE
import brs.api.http.common.ResultFields.DECIMALS_RESPONSE
import brs.api.http.common.ResultFields.DESCRIPTION_RESPONSE
import brs.api.http.common.ResultFields.NAME_RESPONSE
import brs.api.http.common.ResultFields.NUMBER_OF_ACCOUNTS_RESPONSE
import brs.api.http.common.ResultFields.NUMBER_OF_TRADES_RESPONSE
import brs.api.http.common.ResultFields.NUMBER_OF_TRANSFERS_RESPONSE
import brs.api.http.common.ResultFields.QUANTITY_QNT_RESPONSE
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

class GetAllAssetsTest : AbstractUnitTest() {

    private lateinit var t: GetAllAssets

    private lateinit var assetExchangeService: AssetExchangeService

    @Before
    fun setUp() {
        assetExchangeService = mockk(relaxed = true)

        t = GetAllAssets(assetExchangeService)
    }

    @Test
    fun processRequest() {
        val firstIndex = 1
        val lastIndex = 2

        val request = QuickMocker.httpServletRequest(
                MockParam(FIRST_INDEX_PARAMETER, firstIndex),
                MockParam(LAST_INDEX_PARAMETER, lastIndex)
        )

        val mockAssetId: Long = 1

        val mockAsset = mockk<Asset>(relaxed = true)
        every { mockAsset.id } returns 1L
        every { mockAsset.id } returns mockAssetId
        every { mockAsset.name } returns "name"
        every { mockAsset.description } returns "description"
        every { mockAsset.decimals } returns 1.toByte()
        every { mockAsset.quantity } returns 2L

        val mockAssetIterator = mockCollection(mockAsset)

        every { assetExchangeService.getAllAssets(eq(firstIndex), eq(lastIndex)) } returns mockAssetIterator
        every { assetExchangeService.getAssetAccountsCount(eq(mockAssetId)) } returns 1
        every { assetExchangeService.getTransferCount(eq(mockAssetId)) } returns 2
        every { assetExchangeService.getTradeCount(eq(mockAssetId)) } returns 3

        val result = t.processRequest(request) as JsonObject
        assertNotNull(result)

        val assetsResult = result.get(ASSETS_RESPONSE) as JsonArray
        assertNotNull(assetsResult)
        assertEquals(1, assetsResult.size().toLong())

        val assetResult = assetsResult.get(0) as JsonObject
        assertNotNull(assetResult)

        assertEquals(mockAsset.name, assetResult.get(NAME_RESPONSE).safeGetAsString())
        assertEquals(mockAsset.description, assetResult.get(DESCRIPTION_RESPONSE).safeGetAsString())
        assertEquals(mockAsset.decimals.toLong(), assetResult.get(DECIMALS_RESPONSE).safeGetAsLong())
        assertEquals(mockAsset.quantity.toString(), assetResult.get(QUANTITY_QNT_RESPONSE).safeGetAsString())
        assertEquals(mockAsset.id.toString(), assetResult.get(ASSET_RESPONSE).safeGetAsString())
        assertEquals(1L, assetResult.get(NUMBER_OF_ACCOUNTS_RESPONSE).safeGetAsLong())
        assertEquals(2L, assetResult.get(NUMBER_OF_TRANSFERS_RESPONSE).safeGetAsLong())
        assertEquals(3L, assetResult.get(NUMBER_OF_TRADES_RESPONSE).safeGetAsLong())
    }

}
