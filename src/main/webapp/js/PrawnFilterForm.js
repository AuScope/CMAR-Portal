/**
 * Builds a form panel for prawn Layers
 *
 */

PrawnFilterForm = function(record) {

    //-----------Panel
    PrawnFilterForm.superclass.constructor.call(this, {
        id          : 'my-id-' + String.format('{0}',record.get('id')),
        border      : false,
        autoScroll  : true,
        hideMode    : 'offsets',
        //width       : '100%',
        labelAlign  : 'right',
        bodyStyle   : 'padding:5px',
        autoHeight:    true,
        layout: 'anchor',
        items:[ {
            xtype      :'fieldset',
            title      : 'Date Filter',
            autoHeight : true,
            items      : [
            {
                anchor: '100%',
                xtype: 'datefield',
                fieldLabel: 'Start Date',
                name: 'startDate',
                format: 'Y-m-d',
                value: '2007-01-01'
            },
            {
                anchor: '100%',
                xtype: 'datefield',
                fieldLabel: 'End Date',
                name: 'endDate',
                format: 'Y-m-d',
                value: '2008-01-01'
            }]
        }]

    });
};

Ext.extend(PrawnFilterForm, Ext.FormPanel, {

});