/**
 * KenyaUI main javascript library
 */

// Some code still uses jq
jq = jQuery;

/**
 * Configure AngularJS
 */
angular.module('kenyaui', [])
	.filter('keDateTime', function($filter) {
		return function(input) {
			return $filter('keDate')(input) + ' ' + $filter('keTime')(input);
		};
	})
	.filter('keDate', function($filter) {
		return function(input) {
			return $filter('date')(input, 'dd-MMM-yyyy');
		};
	})
	.filter('keDateShort', function($filter) {
		return function(input) {
			return $filter('date')(input, 'MMM-yyyy');
		};
	})
	.filter('keTime', function($filter) {
		return function(input) {
			return $filter('date')(input, 'HH:mm');
		};
	})
	.filter('keDateAuto', function($filter) {
		return function(input) {
			return kenyaui.isToday(new Date(input)) ? $filter('keTime')(input) : $filter('keDate')(input);
		};
	})
	.filter('keGender', function() {
		return function(input) {
			if (input == 'm') {
				return '\u2642 Male';
			}
			else if (input == 'f') {
				return '\u2640 Female';
			}
			return input;
		};
	});

/**
 * Page initialization tasks
 */
jQuery(function() {
	/**
	 * Clicking on a stack-item should direct you to the URL specified in the clickUrl hidden input
	 */
	jQuery('.ke-stack-item').click(function(evt) {
		var clickUrl = jQuery(this).find('input[name=clickUrl]').first();
		var url = (clickUrl.length > 0) ? clickUrl.val() : null;
		if (url) {
			location.href = url;
		}
	});

	/**
	 * Disable autocomplete on all text inputs
	 */
	jQuery('input[type=text]').attr('autocomplete', 'off');

	/**
	 * Initialize search widgets
	 */
	jQuery('.ke-search').each(function() {
		var searchType = jQuery(this).data('searchtype');
		var searchParams = jQuery(this).data('searchparams');
		var searchConfig = kenyaui.getSearchConfig(searchType);

		if (searchParams) {
			searchParams = kenyaui.deparam(searchParams.replace('&amp;', '&'));
		}

		if (searchConfig) {
			jQuery(this).select2({
				placeholder: 'Search for a ' + searchType,
				minimumInputLength: 3,
				ajax: {
					url: ui.fragmentActionLink(searchConfig.searchProvider, searchConfig.searchFragment, searchType + 's'),
					dataType: 'json',
					data: function (term, page) { return jQuery.extend({}, { q: term }, searchParams); },
					results: function (data, page) { return { results: data }; }
				},
				formatResult: function(object, container, query) { return searchConfig.format(object); },
				formatSelection: function(object, container) { return searchConfig.format(object); },
				initSelection: function(element, callback) {
					var id = jQuery(element).val();
					if (id !== '') {
						jQuery.ajax(ui.fragmentActionLink(searchConfig.searchProvider, searchConfig.searchFragment, searchType), { data: { id: id }, dataType: 'json'
						}).done(function(data) { callback(data); });
					}
				},
				escapeMarkup: function (m) { return m; } // don't escape
			});
		}
		else {
			alert('Search type "' + searchType + '" not configured');
		}
	});

	/**
	 * Optimize the size of modal content when browser is re-sized
	 */
	jQuery(window).resize(function() {
		kenyaui.optimizeModalContent();
	});
});

/**
 * Utility methods
 */
(function(kenyaui, $) {

	// For generating unique element ids
	var next_generated_id = 0;

	// Search configurations
	var searchConfigs = new Object();

	/**
	 * Opens a generic modal dialog
	 * @param html the content
	 */
	function openModalContent(html, width, height) {
		if ($('.ke-modal-overlay').length == 0) {
			$('body').append('<div class="ke-modal-overlay"></div>'); // Covers the entire page

			var vspace = height ? (50 - height / 2) : 25;
			var hspace = width ? (50 - width / 2) : 25;
			var style = 'top: ' + vspace + '%; right: ' + hspace + '%; bottom: ' + vspace + '%; left: ' + hspace + '%;';

			$('body').append('<div class="ke-modal-content" style="' + style + '">' + html + '</div>');
		}
	}

	/**
	 * Closes any modal dialog
	 * @param html the content
	 */
	function closeModalContent() {
		// Clear any modal content
		$('.ke-modal-overlay').remove();
		$('.ke-modal-content').remove();
	}

	/**
	 * Creates a button control
	 * @param id the button id
	 * @param label the label
	 */
	function createButton(id, label, icon) {
		var html = '<button type="button" id="' + id + '">';
		if (icon) {
			html += '<img src="' + ui.resourceLink('kenyaui', 'images/glyphs/' + icon + '.png') + '" /> '
		}
		html += label + '</button>';
		return html;
	}

	/**
	 * Gets the browser and version of the client
	 * @returns [ name, version ]
	 */
	kenyaui.getBrowser = function() {
		var N = navigator.appName, ua = navigator.userAgent, tem;
		var M = ua.match(/(opera|chrome|safari|firefox|msie)\/?\s*(\.?\d+(\.\d+)*)/i);
		if (M && (tem = ua.match(/version\/([\.\d]+)/i))!= null) {
			M[2] = tem[1];
		}
		return M ? [M[1], M[2]] : [N, navigator.appVersion,'-?'];
	};

	/**
	 * Updates the model of a AngularJS controller
	 * @param controller the controller element id
	 * @param update the update function
	 */
	kenyaui.updateController = function(controllerId, update) {
		var scope = angular.element(document.getElementById(controllerId)).scope();
		scope.$apply(update(scope));
	};

	/**
	 * Takes an existing form and sets it up to submit via AJAX and get a json response.
	 * @param formId the form id
	 * @param options:
	 * - onSuccess (required) should should be a one-arg function that called with a parsed json object
	 */
	kenyaui.setupAjaxPost = function(formId, options) {
		if (typeof options.onSuccess !== 'function') {
			throw "onSuccess is required";
		}

		$('#' + formId).submit(function(event) {
			event.preventDefault();
			var form = $(this);

			// Disable any submit buttons
			form.find('[type="submit"]').prop('disabled', true);

			// Clear any existing errors
			kenyaui.clearFormErrors(formId);

			// POST and get back the result as JSON
			$.post(form.attr('action'), form.serialize(), null, 'json')
				.done(function(data) {
					options.onSuccess(data);
				})
				.fail(function(xhr) {
					kenyaui.showFormErrors(formId, xhr.responseText);
				})
				.always(function() {
					// Re-enable any submit buttons
					form.find('[type="submit"]').prop('disabled', false);
					if (options.always) {
						options.always();
					}
				});
		});
	};

	/**
	 * Opens a modal loading dialog
	 * @param options the options
	 */
	kenyaui.openLoadingDialog = function(options) {
		var defaults = { heading: null, message: null };
		var options = options ? $.extend(defaults, options) : defaults;

		var html = '<div class="ke-panel-content" style="text-align: center; padding: 10px"><img src="' + ui.resourceLink('kenyaui', 'images/loading.gif') + '"/>';
		if (options.message) {
			html += '<br /><br />' + options.message;
		}
		html += '</div>';

		kenyaui.openPanelDialog({ heading: options.heading, content: html, width: 40, height: 20 });
	};

	/**
	 * Opens a modal confirm (OK/Cancel) dialog
	 * @param options the options
	 */
	kenyaui.openConfirmDialog = function(options) {
		var defaults = {
			heading: null, message: '',
			okLabel: 'OK', cancelLabel: 'Cancel',
			okIcon: 'ok', cancelIcon: 'cancel',
			okCallback: function(){}, cancelCallback: function(){}
		};

		options = options ? $.extend(defaults, options) : defaults;

		var okButtonId = kenyaui.generateId();
		var cancelButtonId = kenyaui.generateId();

		var html = '<div class="ke-panel-content" style="padding: 10px">' + options.message + '</div>';
		html += '<div class="ke-panel-controls">';
		html += createButton(okButtonId, options.okLabel, options.okIcon) + '&nbsp;';
		html += createButton(cancelButtonId, options.cancelLabel, options.cancelIcon);
		html += '</div>';

		kenyaui.openPanelDialog({ heading: options.heading, content: html, width: 40, height: 20 });

		$('#' + okButtonId).click(function() { options.okCallback(); kenyaui.closeDialog(); });
		$('#' + cancelButtonId).click(function() { options.cancelCallback(); kenyaui.closeDialog(); });
	};

	/**
	 * Opens a modal alert (OK) dialog
	 * @param options the options
	 */
	kenyaui.openAlertDialog = function(options) {
		var defaults = {
			heading: null, message: '',
			okLabel: 'OK', okIcon: null,
			okCallback: function(){}
		};

		options = options ? $.extend(defaults, options) : defaults;

		var okButtonId = kenyaui.generateId();

		var html = '<div class="ke-panel-content" style="padding: 10px">' + options.message + '</div>';
		html += '<div class="ke-panel-footer">' + createButton(okButtonId, options.okLabel, options.okIcon) + '</div>';

		kenyaui.openPanelDialog({ heading: options.heading, content: html, width: 40, height: 20 });

		$('#' + okButtonId).click(function() { options.okCallback(); kenyaui.closeDialog(); });
	};

	/**
	 * Opens a modal dialog whose content is fetch dynamically
	 * @param options the options
	 */
	kenyaui.openDynamicDialog = function(options) {
		var defaults = { heading: null, width: null, height: null, scrolling: false };
		options = options ? $.extend(defaults, options) : defaults;

		var tmpContent = '<div class="ke-panel-content" style="text-align: center; padding: 10px"><img src="' + ui.resourceLink('kenyaui', 'images/loading.gif') + '"/>';

		kenyaui.openPanelDialog({ heading: options.heading, content: tmpContent, width: options.width, height: options.height });

		$.get(options.url, function(html) {
			setTimeout(function(){
				kenyaui.optimizeModalContent();
			}, 200);
			$('.ke-modal-content .ke-panel-content').replaceWith(html);

			if (options.scrolling) {
				kenyaui.optimizeModalContent();
			}
		});
	};

	/**
	 * Opens a modal panel style dialog
	 * @param options the options
	 */
	kenyaui.openPanelDialog = function(options) {
		var defaults = { heading: null, width: null, height: null, scrolling: false };
		options = options ? $.extend(defaults, options) : defaults;

		var template = options.templateId ? $('#' + options.templateId) : null;
		var content = template ? '' : options.content;
		var heading = template ? template.attr('title') : options.heading;

		var html = '<div class="ke-panel-frame">';
		if (heading) {
			html += '<div class="ke-panel-heading">' + heading + '</div>';
		}
		html += content + '</div>';

		openModalContent(html, options.width, options.height);

		// Insert template into dialog
		if (template) {
			template.appendTo($('.ke-modal-content .ke-panel-frame')).show().addClass('ke-dialog-template');
		}

		if (options.scrolling) {
			kenyaui.optimizeModalContent();
		}
	};

	/**
	 * Closes any visible dialog
	 */
	kenyaui.closeDialog = function() {
		// Re-attach and hide dialog template to body if it was used
		var template = $('.ke-modal-content .ke-panel-frame .ke-dialog-template')
		if (template.length) {
			template.hide().appendTo($('body'));
		}

		closeModalContent();
	};

	/**
	 * Optimizes the size of a modal dialog so that the content fills the maximum vertical space
	 */
	kenyaui.optimizeModalContent = function() {
		$('.ke-modal-content').each(function() {
			var modalContent = $(this);
			var panelFrame = $(modalContent).find('.ke-panel-frame');
			var panelContent = $(panelFrame).find('.ke-panel-content');

			var nonContentY = panelFrame.height() - panelContent.height();
			var maxContentY = modalContent.height() - nonContentY;

			panelContent.css('max-height', maxContentY).css('overflow', 'scroll');
		});
	}

	/**
	 * Updates a datetime control after any of its child controls have been changed
	 * @param fieldId the datetime field id
	 * @param hasTime whether field uses time
	 */
	kenyaui.updateDateTimeFromDisplay = function(fieldId, hasTime) {
		kenyaui.clearFieldErrors(fieldId); // clear field errors
		var field = $('#' + fieldId);
		field.val(''); // clear field value

		try {
			var date = $.datepicker.parseDate('dd-M-yy', $('#' + fieldId + '_date').val(), null);
			var hours = hasTime ? $('#' + fieldId + '_hour').val() : '00';
			var minutes = hasTime ? $('#' + fieldId + '_minute').val() : '00';

			if (date) {
				// Format date with time fields
				var timestamp = $.datepicker.formatDate($.datepicker.W3C, date) + ' ' + hours + ':' + minutes + ':00.000';
				field.val(timestamp);
			}
		}
		catch (err) {
			kenyaui.showFieldError(fieldId, 'Invalid date');
		}
	};

	/**
	 * Shows a success notification
	 * @param message the message
	 */
	kenyaui.notifySuccess = function(message) {
		if (message) {
			$().toastmessage('showToast', { text: message, stayTime: 5000, sticky: false, type: 'success' });
		}
	};

	/**
	 * Shows an error notification
	 * @param message the message
	 */
	kenyaui.notifyError = function(message) {
		if (message) {
			$().toastmessage('showToast', { text: message, sticky: true, type: 'error' });
		}
	};

	/**
	 * Generates a unique id suitable for a DOM element
	 * @returns string the id
	 */
	kenyaui.generateId = function() {
		return 'ke-element-' + (++next_generated_id);
	};

	/**
	 * Stores a search configuration
	 * @param searchType the search type, e.g. 'location'
	 * @param config the configuration
	 */
	kenyaui.configureSearch = function(searchType, config) {
		searchConfigs[searchType] = config;
	};

	/**
	 * Gets a search configuration
	 * @param searchType the search type, e.g. 'location'
	 * @returns {*}
	 */
	kenyaui.getSearchConfig = function(searchType) {
		return searchConfigs[searchType];
	};

	/**
	 * Shows errors on a form
	 * @param formId the form id
	 * @param xhr the request object
	 */
	kenyaui.showFormErrors = function(formId, response) {
		var form = $('#' + formId);
		var globalError = form.find('.ke-form-globalerrors');
		try {
			var err = $.parseJSON(response);

			globalError.html('Please fix all errors...').show();

			for (var i = 0; i < err.globalErrors.length; ++i) {
				globalError.append('<div>' + err.globalErrors[i] + '</div>');
			}

			for (key in err.fieldErrors) {
				var fieldId = form.find('[name="' + key + '"]').attr('id');
				var errorMsg =  err.fieldErrors[key].join(', ');

				if (fieldId && kenyaui.hasErrorField(fieldId)) {
					kenyaui.showFieldError(fieldId, errorMsg);
				}
				else {
					globalError.append('<div>' + errorMsg + '</div>');
				}
			}
		} catch (ex) {
			kenyaui.notifyError("Failed " + ex + " (" + response + ")");
		}
	};

	/**
	 * Clears any errors being shown for the given form
	 * @param formId the form id
	 */
	kenyaui.clearFormErrors = function(formId) {
		$('#' + formId + ' .ke-form-globalerrors').html('').hide();
		$('#' + formId + ' .error').html('').hide();
	};

	/**
	 * Checks if the given field has an associated error field
	 * @param fieldId the field id
	 */
	kenyaui.hasErrorField = function(fieldId) {
		return $('#' + fieldId + '-error').length > 0;
	};

	/**
	 * Shows an error message for a field (assumes the field has an associated error field)
	 * @param fieldId the field id
	 * @param message the error message
	 */
	kenyaui.showFieldError = function(fieldId, message) {
		$('#' + fieldId + '-error').append(message + '<br />').show();
	};

	/**
	 * Clears any errors being shown for the given field
	 * @param fieldId the field id
	 */
	kenyaui.clearFieldErrors = function(fieldId) {
		$('#' + fieldId + '-error').html('').hide();
	};

	/**
	 * Updates a search field value
	 * @param fieldId the field id
	 * @param value
	 */
	kenyaui.setSearchField = function(fieldId, value) {
		$('#' + fieldId).select2('val', value);
	};

	/**
	 * Sets a date field value
	 * @param fieldId the field id
	 * @param value the value to check
	 */
	kenyaui.setDateField = function(fieldId, value) {
		$('#' + fieldId).val(jq.datepicker.formatDate(jq.datepicker.W3C, value));
		$('#' + fieldId + '_date').datepicker('setDate', value);
	};

	/**
	 * Sets a radiobutton field value
	 * @param fieldId the field id (a container element)
	 * @param value the value to check
	 */
	kenyaui.setRadioField = function(fieldId, value) {
		$('#' + fieldId + ' input[type="radio"][value="' + value + '"]').prop('checked', true);
	};

	/**
	 * Validates a required field
	 * @param fieldId the field id
	 */
	kenyaui.validateRequiredField = function(fieldId) {
		if (($('#' + fieldId).val() + '').length == 0) {
			kenyaui.showFieldError(fieldId, 'Required');
		}
	};

	/**
	 * Validates an integer field
	 * @param fieldId the field id
	 */
	kenyaui.validateIntegerField = function(fieldId) {
		if (!isValidInteger($('#' + fieldId).val())) {
			kenyaui.showFieldError(fieldId, 'Not an integer');
		}
	};

	/**
	 * Reverse of $.param()
	 * @param query the query
	 */
	kenyaui.deparam = function(query) {
		if (!query) {
			return {};
		}

		var params = {};
		var pairs = query.split('&');

		for (var i = 0; i < pairs.length; i++) {
			if (!pairs[i]) {
				continue;
			}

			var pair = pairs[i].split("=");
			pair[0] = decodeURIComponent(pair[0]);
			pair[1] = decodeURIComponent(pair[1]);

			if (typeof params[pair[0]] === 'undefined') {  // first entry with this name
				params[pair[0]] = pair[1];
			} else if (typeof params[pair[0]] === 'string') { // second entry with this name
				var arr = [ params[pair[0]], pair[1] ];
				params[pair[0]] = arr;
			} else { // third or later entry with this name
				params[pair[0]].push(pair[1]);
			}
		}
		return params;
	};

	/**
	 * Determines if a date is today
	 * @param date the date
	 * @returns {boolean}
	 */
	kenyaui.isToday = function(date) {
		var today = new Date();
		return date.getDate() == today.getDate() && date.getMonth() == today.getMonth() && date.getFullYear() == today.getFullYear();
	}


}( window.kenyaui = window.kenyaui || {}, jQuery ));