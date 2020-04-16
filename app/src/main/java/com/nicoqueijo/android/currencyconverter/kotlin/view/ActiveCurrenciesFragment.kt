package com.nicoqueijo.android.currencyconverter.kotlin.view

import android.animation.LayoutTransition
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.jmedeisis.draglinearlayout.DragLinearLayout
import com.nicoqueijo.android.currencyconverter.R
import com.nicoqueijo.android.currencyconverter.kotlin.model.Currency
import com.nicoqueijo.android.currencyconverter.kotlin.util.Utils
import com.nicoqueijo.android.currencyconverter.kotlin.util.Utils.Order.INVALID
import com.nicoqueijo.android.currencyconverter.kotlin.util.Utils.hide
import com.nicoqueijo.android.currencyconverter.kotlin.util.Utils.show
import com.nicoqueijo.android.currencyconverter.kotlin.viewmodel.ActiveCurrenciesViewModel


class ActiveCurrenciesFragment : Fragment() {

    private lateinit var viewModel: ActiveCurrenciesViewModel

    private lateinit var emptyListView: LinearLayout
    private lateinit var dragLinearLayout: DragLinearLayout
    private lateinit var floatingActionButton: FloatingActionButton
    private lateinit var keyboard: DecimalNumberKeyboard
    private lateinit var scrollView: ScrollView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_active_currencies, container, false)
        viewModel = ViewModelProvider(this).get(ActiveCurrenciesViewModel::class.java)
        initViewsAndAdapter(view)
        observeObservables()
        populateDefaultCurrencies()
        return view
    }

    private fun initViewsAndAdapter(view: View) {
        emptyListView = view.findViewById(R.id.empty_list) // connect this with the DragLinearLayout some way
        dragLinearLayout = view.findViewById(R.id.drag_linear_layout)
        scrollView = view.findViewById(R.id.scroll_view)
        dragLinearLayout.setContainerScrollView(scrollView)
        keyboard = view.findViewById(R.id.keyboard)
        initFloatingActionButton(view)
        if (viewModel.wasListConstructed) {
            restoreActiveCurrencies()
        }
    }

    private fun restoreActiveCurrencies() {
        viewModel.activeCurrencies.value?.forEach { activeCurrency ->
            RowActiveCurrency(activity).run row@{
                currencyCode.text = activeCurrency.trimmedCurrencyCode
                flag.setImageResource(Utils.getDrawableResourceByName(activeCurrency.currencyCode.toLowerCase(), activity))
                conversion.text = activeCurrency.conversion.conversionText
                dragLinearLayout.run {
                    addView(this@row)
                    setViewDraggable(this@row, this@row)
                }
                this.currencyCode.setOnLongClickListener {
                    dragLinearLayout.run {
                        val currencyToRemove = viewModel.memoryActiveCurrencies[dragLinearLayout.indexOfChild(this@row)]
                        currencyToRemove.order = INVALID.position
                        currencyToRemove.isSelected = false
                        viewModel.upsertCurrency(currencyToRemove)
                        viewModel.memoryActiveCurrencies.remove(currencyToRemove)
                        layoutTransition = LayoutTransition()
                        removeDragView(this@row)
                        layoutTransition = null
                        Snackbar.make(this, R.string.item_removed, Snackbar.LENGTH_LONG)
                                .setAction(R.string.undo) {
                                    // handle the undo
                                }.show()
                    }
                    true
                }
            }
        }
    }

    private fun observeObservables() {
        viewModel.activeCurrencies.observe(viewLifecycleOwner, Observer { dbActiveCurrencies ->
            toggleKeyboardVisibility(dbActiveCurrencies)
            if (!viewModel.wasListConstructed) {
                constructActiveCurrencies(dbActiveCurrencies)
            }
            if (wasCurrencyAddedViaFab(dbActiveCurrencies)) {

                val addedCurrency = dbActiveCurrencies.takeLast(1).single()
                viewModel.memoryActiveCurrencies.add(addedCurrency)
                RowActiveCurrency(activity).run row@{
                    currencyCode.text = addedCurrency.trimmedCurrencyCode
                    flag.setImageResource(Utils.getDrawableResourceByName(addedCurrency.currencyCode.toLowerCase(), activity))
                    conversion.text = addedCurrency.conversion.conversionText
                    dragLinearLayout.run {
                        addView(this@row)
                        setViewDraggable(this@row, this@row)
                    }
                    this.currencyCode.setOnLongClickListener {
                        dragLinearLayout.run {
                            val currencyToRemove = viewModel.memoryActiveCurrencies[dragLinearLayout.indexOfChild(this@row)]
                            currencyToRemove.order = INVALID.position
                            currencyToRemove.isSelected = false
                            viewModel.upsertCurrency(currencyToRemove)
                            viewModel.memoryActiveCurrencies.remove(currencyToRemove)
                            layoutTransition = LayoutTransition()
                            removeDragView(this@row)
                            layoutTransition = null
                            Snackbar.make(this, R.string.item_removed, Snackbar.LENGTH_LONG)
                                    .setAction(R.string.undo) {
                                        // handle the undo
                                    }.show()
                        }
                        true
                    }
                }


            }
            if (dbActiveCurrencies.isEmpty()) {
                emptyListView.show()
            } else {
                emptyListView.hide()
            }
            // When currency is added or swiped, add/remove an item from DragLinearLayout
        })
        // When the focused currency changes update the hints
        viewModel.focusedCurrency.observe(viewLifecycleOwner, Observer { focusedCurrency ->
            /*updateHints(focusedCurrency)*/
        })
    }

    /**
     * This indicates user added a currency by selecting it from the SelectableCurrenciesFragment
     * that was initiated by the press of the FloatingActionButton.
     * The indication is triggered when, apart from having one additional element, the
     * [dbActiveCurrencies] is the same as the [memoryActiveCurrencies].
     */
    fun wasCurrencyAddedViaFab(dbActiveCurrencies: List<Currency>): Boolean {
        return viewModel.memoryActiveCurrencies == dbActiveCurrencies.dropLast(1)
    }

    /**
     *
     */
    private fun constructActiveCurrencies(currencies: MutableList<Currency>?) {
        currencies?.forEach { currency ->
            viewModel.memoryActiveCurrencies.add(currency)
            RowActiveCurrency(activity).run row@{
                currencyCode.text = currency.trimmedCurrencyCode
                flag.setImageResource(Utils.getDrawableResourceByName(currency.currencyCode.toLowerCase(), activity))
                conversion.text = currency.conversion.conversionText
                dragLinearLayout.run {
                    addView(this@row)
                    setViewDraggable(this@row, this@row)
                }
                this.currencyCode.setOnLongClickListener {
                    dragLinearLayout.run {
                        val currencyToRemove = viewModel.memoryActiveCurrencies[dragLinearLayout.indexOfChild(this@row)]
                        currencyToRemove.order = INVALID.position
                        currencyToRemove.isSelected = false
                        viewModel.upsertCurrency(currencyToRemove)
                        viewModel.memoryActiveCurrencies.remove(currencyToRemove)
                        layoutTransition = LayoutTransition()
                        removeDragView(this@row)
                        layoutTransition = null
                        Snackbar.make(this, R.string.item_removed, Snackbar.LENGTH_LONG)
                                .setAction(R.string.undo) {
                                    // handle the undo

                                }.show()
                    }
                    true
                }
            }
        }
        viewModel.wasListConstructed = true
    }

    private fun addRowAtEnd() {

    }

    private fun addRowAtIndex(/*index or currency code*/) {

    }

    private fun removeRowAtIndex(/*index or currency code*/) {

    }

    private fun toggleKeyboardVisibility(currencies: MutableList<Currency>) {
        when {
            currencies.isEmpty() -> {
                // Drop down keyboard
            }
            currencies.isNotEmpty() -> {
                // Pop up keyboard
            }
        }
    }

    private fun initFloatingActionButton(view: View) {
        floatingActionButton = view.findViewById(R.id.floating_action_button)
        floatingActionButton.setOnClickListener {
            findNavController().navigate(R.id.action_activeCurrenciesFragment_to_selectableCurrenciesFragment)
        }
    }

    private fun populateDefaultCurrencies() {
        viewModel.populateDefaultCurrencies()
    }

    companion object {

        fun log(message: String) {
            Log.d("Nicoo", message)
        }

        val currencyCodes = listOf(
                "USD_ARS",
                "USD_BRL",
                "USD_CAD",
                "USD_DKK",
                "USD_EUR"
        )
    }
}
