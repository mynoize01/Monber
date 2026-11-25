package com.example.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.CustomerEntity
import com.example.data.model.Di319Row
import com.example.data.repository.CustomerRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CustomerRoomCacheTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: CustomerRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = CustomerRepository(
            customerDao = db.customerDao(),
            di319Dao = db.di319Dao(),
            syncMetadataDao = db.syncMetadataDao()
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun offlineRefreshFailure_preservesCachedCustomersInRoom() = runBlocking {
        val sampleCsv = """
            Report
            Info
            Unit
            Header
            Tanggal,24/11/2026
            CIF,NO REK,NAMA NASABAH,PLAFOND,OS PINJAMAN,KOLEK,NAMA MANTRI,PN MANTRI
            CIF001,123456789012345,BUDI SANTOSO,25000000,15000000,SML1,ANDI,207803
        """.trimIndent()

        // First populate from valid offline CSV content
        val initialCustomers = listOf(
            CustomerEntity(
                namaNasabah = "BUDI SANTOSO",
                plafond = 25_000_000.0,
                osPinjaman = 15_000_000.0,
                kolek = "SML1",
                namaMantri = "ANDI",
                menuType = "INTRAKOMP",
                alamat = "JL MERDEKA NO 10",
                cif = "CIF001",
                kolekBlnLalu = "L",
                noRekening = "123456789012345",
                extraColumns = "Desa::KATAPANG||No HP 1::081234567890",
                pnMantri = "207803"
            )
        )
        db.customerDao().refreshCustomers("INTRAKOMP", initialCustomers)
        repository.updateGlobalSyncMetadata(initialCustomers.size, "INTRAKOMP")

        // Verify Room query and metadata exist
        val cachedBefore = repository.getCustomersFlow("INTRAKOMP").first()
        assertEquals(1, cachedBefore.size)
        assertEquals("BUDI SANTOSO", cachedBefore.first().namaNasabah)
        assertNotNull(repository.getLastSyncMetadata())

        // Attempt network refresh with invalid/unreachable URL -> must preserve cached Room rows
        val failedResult = repository.refreshData(
            menuType = "INTRAKOMP",
            customUrl = "http://127.0.0.1:1/unreachable.csv"
        )
        assertTrue(failedResult.isFailure)

        val cachedAfterFailure = repository.getCustomersFlow("INTRAKOMP").first()
        assertEquals(1, cachedAfterFailure.size)
        assertEquals("BUDI SANTOSO", cachedAfterFailure.first().namaNasabah)
        assertTrue(sampleCsv.contains("BUDI SANTOSO"))
    }

    @Test
    fun copyPeriodToMenu_swapsFromRoomCacheAndPreservesEnrichedFields() = runBlocking {
        val currentIntra = listOf(
            CustomerEntity(
                namaNasabah = "SITI AMINAH",
                plafond = 50_000_000.0,
                osPinjaman = 32_000_000.0,
                kolek = "SML1",
                namaMantri = "DEDI",
                menuType = "INTRAKOMP",
                alamat = "KP SUKAMAJU RT 02",
                cif = "CIF999",
                kolekBlnLalu = "Lancar",
                noRekening = "000123456789012",
                extraColumns = "Desa::SANGKANHURIP||No HP 1::081999888777",
                pnMantri = "112233"
            )
        )
        db.customerDao().refreshCustomers("INTRAKOMP", currentIntra)

        val dailySnapshot = listOf(
            CustomerEntity(
                namaNasabah = "SITI AMINAH",
                plafond = 50_000_000.0,
                osPinjaman = 30_000_000.0,
                kolek = "SML2",
                namaMantri = "DEDI",
                menuType = "PERIOD_DAILY",
                alamat = "",
                cif = "CIF999",
                kolekBlnLalu = "",
                noRekening = "000123456789012",
                extraColumns = "NPD_FULL::15/11/2026",
                pnMantri = "112233"
            )
        )
        repository.savePeriodCustomers("PERIOD_DAILY", dailySnapshot)

        val copied = repository.copyPeriodToMenu("PERIOD_DAILY", "INTRAKOMP")
        assertTrue(copied)

        val swapped = repository.getCustomersFlow("INTRAKOMP").first()
        assertEquals(1, swapped.size)
        assertEquals(30_000_000.0, swapped.first().osPinjaman, 0.01)
        assertEquals("SML2", swapped.first().kolek)
        assertEquals("KP SUKAMAJU RT 02", swapped.first().alamat)
        assertEquals("Lancar", swapped.first().kolekBlnLalu)
        assertTrue(swapped.first().extraColumns.contains("Desa::SANGKANHURIP"))
    }

    @Test
    fun di319Rows_savedAndQueriedOfflineFromRoom() = runBlocking {
        val rows = listOf(
            Di319Row(cif = "CIF001", noRekening = "999000111", namaNasabah = "BUDI", saldo = 1_500_000.0)
        )
        repository.saveDi319Rows(rows)

        val cached = repository.getCachedDi319RowsOnce()
        assertEquals(1, cached.size)
        assertEquals(1_500_000.0, cached.first().saldo, 0.01)
        assertEquals("DI319", repository.getLastSyncMetadata()?.label)
    }
}
