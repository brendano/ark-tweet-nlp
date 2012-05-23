<?php echo '<?xml version="1.0" encoding="UTF-8" ?>'; 

/**
TweetNLP POS Annotation Interface

A simple web browser interface for labeling words of text, especially tweets from 
Twitter, with part-of-speech tags. Built for Firefox 3.6+ using PHP and Javascript.

SETUP INSTRUCTIONS
1 Copy the contents of this directory to a web server with PHP installed.
2 Optionally modify the tagset by changing constant definitions below. 
  This script assumes that each tag will be a single ASCII character.
  VALID_LABELS indicates the set of valid keypresses and LABEL_NAMES 
  describes the (case-insensitive) tags.
3 Preprocess raw data by tokenizing it and (optionally) running an automatic tagger. 
  See sample.automatic_tags, each line of which contains a tweet ID, tokens, 
  initial tags, and metadata.
4 Create two empty output files: for the "sample" dataset, these will be
  sample.annotation.tags - each line will have fields for the tweet ID, 
     a problem flag ("Tokenization Error", "Not English", or "Garbage"), tokens, 
     annotated tags, and annotation metadata (the submission time and the username, 
     if this is known to PHP in the REMOTE_USER server variable).
  sample.annotation.log - this will record individual user interface actions
  Make sure permissions are such that this script is able to append to the output files.
5 Apportion annotation batch URLs to annotators. Each batch is determined in terms 
  of line offsets to the input file. For example,
     http://server.path/pos.php?split=sample&from=0&to=10&perpage=5
  indicates that the batch contains the first 10 lines of sample.automatic_tags, 
  and should be displayed in two pages of 5 items each.

Note that each time an annotator submits a page, output will be appended to the 
.annotation.tags and .annotation.log files. In particular, the order of output 
items will probably not correspond to the input order, so item IDs should be used 
to associate output with metadata in the input file.

@author: Nathan Schneider (nathan@cmu.edu)
@since: 2011-01-16
*/

?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<style type="text/css">
	h1,h2 { text-align: center; }
	h1,h2,.instructions,label { font-family: verdana,sans-serif; }
	h2 { background-color: #ddd; width: 90%; margin-left: auto; margin-right: auto; margin-top: 40px; }
	.instructions { max-width: 45em; background-color: #cfc; margin-left: auto; margin-right: auto; padding: 10px; }
	.instructions.notready { background-color: #fcc; }
	ol.lAR,ol.lHE,div.lAR,div.lHE { direction: rtl; }
	
	/* Center the sentences list as a whole with its width automatically sized to fit contents (http://matthewjamestaylor.com/blog/beautiful-css-centered-menus-no-hacks-full-cross-browser-support) */
	ol#sentences { clear:left; float:left; left:50%; position:relative; width: 70%; }
	ol#sentences + * { clear: both; }
	ol#sentences.singleton { list-style-type: none; padding-left: 0; }
	ol#sentences li { clear:left; float:left; position:relative; right:50%; width: 100%; }
	form { overflow: hidden; } /* avoid horizontal scrolling due to the centering trick */
	
	ol#q0sentences { list-style-type: none; overflow: auto; }
	ol#q0sentences li { width: 1100px; }
	ol#q0sentences li * { vertical-align: middle; }
	ol#exemplars { list-style-type: none; }
	p.sentence.exemplar { margin-left: 60px; margin-right: 60px; }
	p.sentence { font-size: 16pt; max-width: 90%; word-spacing: 10px; margin-top: 0.5em; margin-bottom: 0.5em; line-height: 200%; }
	p.sentence.split { font-size: 24pt; color: #333; }
	p.sentence.lEN,#sentences { font-family: arial,helvetica,sans-serif; }
	p.sentence.lEN { font-size: 20pt;  }
	p.sentence .word.active { color: #e90; }
	p.sentence.split span.word { padding: 0 0.5em; }
	p.sentence.split.lEN { font-family: arial,helvetica,sans-serif; }
	span:focus.word { color: #0af; /*font-weight: bold;*/ }
	span.word.eligible:hover { color: #fc6; cursor: pointer; }
	
	.instructions a#more,.instructions a#less { color: #777; text-decoration: none; }
	
	.question { text-align: center; }
	/* Center as with #sentences */
	#answerOptions { list-style-type: none; padding-left: 0; margin-top: 0; clear:left; float:left; left:50%; position:relative; }
	#answerOptions + * { clear: both; }
	#answerOptions li { clear:left; float:left; position:relative; right:50%; }
	#answerOptions label { color: #000; }
	
	textarea#results { display: block; }
	
	.controls label,.controls input { vertical-align: top; }
	.controls label { display: block; margin-left: auto; margin-right: auto; text-align: center; color: #777; }
	.controls input { display: inline-block; }
	
	.word { position: relative; }
	.label { position: absolute; left: 0; top: -1.7em; display: block; width: 100%; text-align: center; font-size: 75%; }
	*[contenteditable=true] { cursor: text !important; }
	
	.default { color: #777 !important; }	/* The current label is a default; the user has not specified one */
	.manual { color: #000; }	/* The user has specified a label (regardless of whether there is a default: may be combined with .changed) */
	.nondefault { color: #c00 !important; }	/* The current label has been changed to something other than the default */
	
	.metadata { display: none; }
	.itemControls { position: absolute; left: -11em; width: 11em; top: 2em; }
	
	.small { font-size: small; }
</style>
<?

include_once("json.php");

$lang = "EN";

$iFrom = intval($_REQUEST['from']);
$iTo = intval($_REQUEST['to']);

if (isset($_REQUEST['split']))
	$split = $_REQUEST['split'];
else
	$split = 'dev';

$perpage = intval($_REQUEST['perpage']);
if ($perpage<=0)
	$perpage = 10;

if (array_key_exists("submit", $_REQUEST)) {	// save data from a page of annotation
	$LOG_FILE = "$split.annotation.log";
	$TAG_FILE = "$split.annotation.tags";
	$logF = fopen($LOG_FILE, 'a');
	$tagF = fopen($TAG_FILE, 'a');
	if (!$logF || !$tagF) die("Unable to save annotations in files in " . getcwd());
	fwrite($logF, htmlspecialchars_decode(stripslashes($_REQUEST['resultsLog']), ENT_QUOTES));
	fwrite($tagF, htmlspecialchars_decode(stripslashes($_REQUEST['resultsTags']), ENT_QUOTES));
	fclose($logF);
	fclose($tagF);
}

($iFrom>=0 && $iTo>$iFrom) or die("You have finished annotating the current batch. Thanks!");

$IN_FILE = "$split.automatic_tags";
$f = fopen($IN_FILE, 'r');

// The sentences. Word tokens are space-separated and have default tags.
// $SENTENCES = array('Mark/^ Owens/^ will/M be/V chairing/V the/D session/N with/P Dr./^ Phelps/^ ./,', 
//					'Down/N is/V neither/X as/P warm/A as/P fur/N ,/, nor/X as/P expensive/A ./,');
$SENTENCES = array();

if ($f) {
	$l = 0;
    while (($entry = fgets($f)) !== false) {
        if ($l >= $iFrom) {
			if ($l >= $iTo || $l >= ($iFrom+$perpage)) break;

			$entry = htmlspecialchars($entry, ENT_QUOTES);	
			$entry = explode("\t", $entry);
			$twitterId = $entry[0];
			$tokenizedS = $entry[1];
			$tagsS = $entry[2];
			$metadata = "<b>$twitterId</b> " . join("@@", array_slice($entry, 3));	//json_indent($entry[3]);
			$tokens = explode(' ', $tokenizedS);
			$tags = explode(' ', $tagsS);
			$taggedS = "";
			for ($i=0; $i<count($tokens); $i++)
				$taggedS .= "$tokens[$i]/$tags[$i] ";

			array_push($SENTENCES, array('sentence' => trim($taggedS), 'twitterId' => $twitterId, 'metadata' => $metadata));
		}
		$l++;
    }
    fclose($f);
}

?>
<script type="text/javascript" language="javascript" src="http://code.jquery.com/jquery-1.4.2.min.js"></script>
<script type="text/javascript">
function getClass(classString, prefix) {
	var c = " " + classString + " ";
	var i = c.indexOf(" "+prefix);
	if (i==-1)
		return "";
	return c.substring(i+1, c.indexOf(" ", i+1));
}

// Escape special HTML characters, including quotes
function htmlspecialchars(s) {
	return $("<div>").text(s).html().replace(/"/g, '&quot;').replace(/'/g, '&#039;');
}


//Cookie-handling functions
//Source: http://www.quirksmode.org/js/cookies.html, 24 Aug 2008

function createCookie(name,value,days) {
	if (days) {
		var date = new Date();
		date.setTime(date.getTime()+(days*24*60*60*1000));
		var expires = "; expires="+date.toGMTString();
	}
	else var expires = "";
	document.cookie = name+"="+value+expires+"; path=/";
}

function readCookie(name) {
	var nameEQ = name + "=";
	var ca = document.cookie.split(';');
	for(var i=0;i < ca.length;i++) {
		var c = ca[i];
		while (c.charAt(0)==' ') c = c.substring(1,c.length);
		if (c.indexOf(nameEQ) == 0) return c.substring(nameEQ.length,c.length);
	}
	return null;
}

function eraseCookie(name) {
	createCookie(name,"",-1);
}

function equivArrays(x, y) {
	if (x.length!=y.length) return false;
	for (var i=0; i<x.length; i++) {
		if (x[i]!=y[i]) return false;
	}
	return true;
}

// The provided string will be conjoined with other fields to form a CSV record, and should thus be appropriately quoted
function logString(s) {
	var logEntry = new String(new Date());
	if (s!=null)
		logEntry += "," + s;
	$("#results").val($("#results").val() + logEntry + "\n");
}
function logEvent(targetId, att, val) {	// 'val', in particular, should be appropriately quoted
	logString('<?= $_SERVER['REMOTE_USER'] ?>,' + targetId + ',' + att + ',' + val);
}
// Record a user's decision to add or remove a segmentation point
// segmentationPoint: e.g. s85c5 (where character indices to the tokenized sentence are 0-based and include spaces)
// addition: true if the segmentation is being added, false otherwise
function recordDecision(segmentationPosition, addition) {
	logString(((addition) ? '+' : '-') + segmentationPosition);
}
// Record that the user clicked to see more of the instructions, suggesting that they're paying attention
function recordClickedMore() {
	logString("expandInstructionDetails");
}

function shuffleList(listId) {
	// Randomize the ordering of the items in the given list, and return the list item IDs in their new order as an Array
	// - Remove sentences from the DOM
	var lis = $("#" + listId + " > li").detach();

	// - Generate a permuation of the integers from 0 to the number of sentences
	var numsSorted = new Array();
	for (var h=0; h<lis.length; h++)
		numsSorted[h] = h;
	var numsRandomized = new Array();
	for (var q=0; numsSorted.length>0; q++) {
		var r = Math.floor(Math.random()*numsSorted.length);
		// Remove the rth number and put it in the randomized array
		var rem = numsSorted.splice(r,1);
		numsRandomized.push(rem[0]);
	}
	
	// - Add sentences back to the DOM in the randomized order
	newOrder = new Array();
	for (var qq=0; qq<numsRandomized.length; qq++) {
		var liId = lis.eq(numsRandomized[qq]).appendTo("#"+listId).attr("id");
		newOrder.push(liId);
	}

	return newOrder;
}

KEY_LEFT_ARROW = 37;
KEY_UP_ARROW = 38;
KEY_RIGHT_ARROW = 39;
KEY_DOWN_ARROW = 40;

VALID_LABELS = 'NnOoSs^ZzLlMmVvAaRr!DdPp&TtXxYy#@~UuEe$,Gg?';	
LABEL_NAMES = {
		'N': 'common noun', 
		'O': 'pronoun (personal or WH, not possessive)',
		'S': 'nominal + possessive',
		'^': 'proper noun', 
		'Z': 'proper noun + possessive',
		'L': 'nominal + verbal',
		'M': 'proper noun + verbal',
		'V': 'verb or auxiliary', 
		'A': 'adjective', 
		'R': 'adverb', 
		'!': 'interjection', 
		'D': 'determiner', 
		'P': 'preposition or subordinating conjunction', 
		'&': 'coordinating conjunction', 
		'T': 'verb particle', 
		'X': 'existential "there" or predeterminer', 
		'Y': 'X + verbal',
		'#': 'hashtag', 
		'@': 'at-mention', 
		'~': 'Twitter discourse function word', 
		'U': 'URL or email address', 
		'E': 'emoticon', 
		'$': 'numeral', 
		',': 'punctuation', 
		'G': 'other abbreviation, foreign word, possessive ending, symbol, or garbage',
		'?': 'unsure'};

function keyPressLabel(e) {  // Interprets a key press, returning the (valid) label indicated by the keypress, or null otherwise
	if (!(e.ctrlKey || e.altKey || e.metaKey || e.keyCode==13 || e.keyCode==10 || e.keyCode==9) 
		//&& !(e.target && ((e.target.nodeName.toLowerCase()=="input" && (e.target.type.toLowerCase()=="text" || e.target.type.toLowerCase()=="password")) 
		//		|| e.target.nodeName.toLowerCase()=="textarea" || e.target.nodeName.toLowerCase()=="select"))
		) {
		//if (!e.shiftKey) {
			var code = e.which;
			if (code==0) code = e.keyCode;	// For control keys (e.g. arrow keys)
			var key = String.fromCharCode(code);
			var lbl = key;
			if (VALID_LABELS.indexOf(key)==-1)	// key does not correspond to a valid label
				lbl = null;
			if (e.preventDefault) { // Prevent Firefox from using Find-As-You-Type
				e.preventDefault();
			}
		//}

		if (lbl!=null)
			return lbl.toUpperCase();
	}
	return null;
}


function init() {
	if (!$.support.boxModel) {
		var msg = 'A recent browser such as the latest version of <a href="http://mozilla.org/firefox">Firefox</a>, Safari, or Chrome is required for this task.';
		//if ($.browser.msie)
		//	msg += ' Internet Explorer may NOT be used for this task.';
		$("body").html('<div class="instructions notready"><p>'+msg+'</p></div>');
		return;
	}

	// Read/create cookie indicating the user's exemplar sentence condition
	var cookieName = "MTurkParse_<?= $lang ?>_condition";
	
	
	// Process the sentences
	
	function decorateSentences(selector) {
		var giw = 1;	// global word index across all sentences on the page. Must be nonzero (used for tabIndex).
		$(selector).each(
				function (j) {
					var sTag = $(this).attr("id");	// sentence tag, e.g. s3
					var t = htmlspecialchars($(this).find("p.original").text()) + " ";
					var oclass = $(this).find("p.original").attr("class");
					var w = "";	// current word
					var iw = 1;	// word index (1-based)
					var wt = '';	// word-split text
					
					for (var i=0; i<t.length; i++) {
						var c = t.charAt(i);
						if (c==" ") {	// word break
							var p = "";	// trailing punctuation
							/*if (w.indexOf("|")>-1) {	// delimits trailing punctuation that shouldn't be counted as part of the word (e.g. "right|?" but "Dr.")
								p = w.substring(w.indexOf("|")+1);
								w = w.substring(0,w.indexOf("|"));
							}*/
							var status = /*(iw==<?= $t ?>) ? "target" :*/ "eligible";

							//if (i==t.length-1)
							//	iw = 0;	// the "NONE" option, analogous to the ROOT/WALL symbol
							// ^^^^ not sure why this was here--it was making the index of the last word of each tweet 0

							w = $("<div>"+w+"</div>").text();	// unescape HTML special chars
							if (w.slice(-2)[0]!="/")
								logString("Invalid word: " + w);
							//wt += '<span class="word '+sTag+'w'+iw+' '+status+'">' + w + '</span>' + p + " ";
							var lbl = htmlspecialchars(w.slice(-1));
							var wS = htmlspecialchars(w.slice(0,-2));
							
							// The display form of the word and its label. (The label will be positioned above the word, but must follow it in HTML so text wrapping works.)
							wt += '<span class="word '+sTag+'w'+iw+' '+status+'" tabindex="' + giw + '"><span class="token">' + wS + '</span><span class="label default' + lbl + ' default" title="' + LABEL_NAMES[lbl] + '"><span>' + lbl + '</span></span></span>' + p + " ";
							w = "";
							iw++;
							giw++;
						}
						else {
							w += c;
						}
					}
					$(this).find('p.original').after('<div class="itemControls"><select><option></option><option>Tokenization Error</option><option>Not English</option><option>Garbage</option></select> <button class="btnMetadata">Metadata</button></div>').replaceWith('<p class="'+oclass+'">' + wt + '</p>');
					$(this).find('.btnMetadata').click(function (evt) {
						$(this).parent().siblings('.metadata').toggle();
						if (evt.preventDefault)
							evt.preventDefault();
						logEvent($(this).parent().parent().attr("id"), "toggleMetadata", $(this).parent().siblings('.metadata').css('display'));
					});
					$(this).find('.itemControls select').change(function () {
						if ($(this).val()!="" && $(this).val()!="Tokenization Error")
							$(this).parent().siblings('p.original').css('text-decoration','line-through').css('font-size','15pt');
						else
							$(this).parent().siblings('p.original').css('text-decoration','inherit').css('font-size','');
						logEvent($(this).parent().parent().attr("id"), "changeSentenceFlag", '"' + $(this).val() + '"');
					});
				});
	}
	
	var coordinate = function(phrases) {	// Conjoin multiple phrases with 'and' and commas/semicolons where necessary
		if (phrases.length==0)
			return "";
		if (phrases.length==1)
			return phrases[0];
		if (phrases.length==2) {
			var p01 = phrases[0] + " " + phrases[1];
			if (p01.indexOf(", and ")>-1)
				return phrases[0] + "; and " + phrases[1];
			else if (p01.indexOf(" and ")>-1)
				return phrases[0] + ", and " + phrases[1];
		}
		var s = "";
		var semicolon = false;
		for (var i=0; i<phrases.length; i++) {
			if (phrases[i].indexOf(", and ")>-1)
				semicolon = true;
		}
		for (var i=0; i<phrases.length-1; i++)
			s += phrases[i] + ((semicolon) ? "; " : ", ");
		s += "and " + phrases[phrases.length-1];
		return s;
	}
	
	
	decorateSentences("#sentences > li");
			
	var enactRecording = function (listId, prefix) {
		$("#" + listId + " span.word").click(function (evt) {
			if (!$(this).hasClass("ignoreme")) {
				$(this).focus();
			}
		});
		$("#" + listId + " span.word").keypress(function (evt) {
			/*if (evt.keyCode==10 || evt.keyCode==13) {	// toggle contentEditable
				
				var tknSpan = $(this).find('.token');
				var tagSpan = $(this).find('.label span');	// .label is absolutely positioned, so we don't want to make it contenteditable (Firefox would display annoying resize handles)
				if (tagSpan.attr("contenteditable")=='true' || tknSpan.attr("contenteditable")=='true') {
					tknSpan.blur();
					tagSpan.blur();
				}
				else {	// make only the token itself editable (not the tag)
					tknSpan.attr("contenteditable", 'true');
					tknSpan.focus();
					tknSpan.blur(function (e) { $(this).attr("contenteditable", 'inherit'); });
				}
				return;
			}
			else*/ if ($(this).find('.token').attr("contentEditable")=='true') {
				if (evt.keyCode==KEY_UP_ARROW) {
					$(this).find('.token').blur();
					// make the tag independently editable
					var tagSpan = $(this).find('.label span');
					tagSpan.attr("contenteditable", (tagSpan.attr("contenteditable")==null || tagSpan.attr("contenteditable")=='inherit') ? 'true' : 'inherit');
					tagSpan.focus();
					tagSpan.blur(function (e) { $(this).attr("contenteditable", 'inherit'); });
					if (evt.preventDefault) evt.preventDefault();
				}
				return;	// default to normal behavior, i.e. editing the content
			}
			else if ($(this).find('.label span').attr("contentEditable")=='true') {
				if (evt.keyCode==KEY_DOWN_ARROW) {
					$(this).find('.label span').blur();
					// make only the token itself editable (not the tag)
					var tknSpan = $(this).find('.token');
					tknSpan.attr("contenteditable", (tknSpan.attr("contenteditable")==null || tknSpan.attr("contenteditable")=='inherit') ? 'true' : 'inherit');
					tknSpan.focus();
					tknSpan.blur(function (e) { $(this).attr("contenteditable", 'inherit'); });
					if (evt.preventDefault) evt.preventDefault();
				}
				return;	// default to normal behavior, i.e. editing the content
			}
			
			var lbl = keyPressLabel(evt);
			
			if (lbl!=null) {
				var lblElt = $(this).find(".label");
				lblElt.text(lbl).removeClass("default").addClass("manual").attr("title", LABEL_NAMES[lbl]);
				if (!lblElt.hasClass("default"+lbl))
					lblElt.addClass("nondefault");
				logEvent(getClass(this.className, "s"), "label", '"'+lbl+'"');
			}
			else if (evt.keyCode==KEY_RIGHT_ARROW) {	// Simulate Tab
				$("[tabindex=" + (this.tabIndex+1) + "]").focus();
			}
			else if (evt.keyCode==KEY_LEFT_ARROW) {	// Simulate Shift+Tab
				$("[tabindex=" + (this.tabIndex-1) + "]").focus();
			}
		});

		$("#" + listId + " span.word").focus(function (evt) {
			logEvent(getClass(this.className, "s"), "focus", "");
		});
	};
	enactRecording("sentences", "s");
	

	var validate = function (evt) {
		// Generate simple results output: current labels for each word along with the sentence id and flag
		var res = "";
		$("#sentences > li").each(function (i) {
			var meta = $(this).find(".metadata").text();
			var twitterId = meta.substring(0, meta.indexOf(" "));
			var flag = $(this).find(".itemControls select").val();
			var words = '';
			var labels = '';
			$(this).find(".word").each(function (j) {
				var wordAndLabel = htmlspecialchars($(this).text());
				var label = htmlspecialchars($(this).find(".label").text());
				
				var word = wordAndLabel.slice(0, -label.length);
				words += word.trim().replace(/\s+/g,'▪') + ' ';
				labels += label.trim().replace(/\s+/g,'▪') + ' ';
			});
			res += twitterId + '\t' + flag + '\t' + words.slice(0, -1) + '\t' + labels.slice(0, -1) + '\t' + '<?= $_SERVER['REMOTE_USER'] ?>' + '\t' + new String(new Date()) + '\n';
		});
		$("#resultsTags").val(res);

		logEvent("", "submitPage", "");
		
		return true;
	}
	$("form").submit(validate);

	logEvent("", "pageReady", "<?= "$iFrom:$iTo:$perpage" ?>");
	$("#sentences > li").each(function (i) {
		var meta = $(this).find(".metadata").text();
		var twitterId = meta.substring(0, meta.indexOf(" "));
		logEvent(this.id, "twitterId", twitterId);
	});
}

$("body").ready(init);
</script>
<title>Twitter POS Annotation (<?= $_SERVER['REMOTE_USER'] ?>)</title>
</head>
<body>
<form action="" method="post">
<h1>Twitter POS Annotation</h1>

<div class="instructions">
	<p>Review the output of an automatic POS tagger run on the sentences below. Click on a word and type the correct tag to change it from the default. <a href="annotator-instructions.txt">(complete guidelines)</a></p>
</div>
<ol id="sentences" class="l<?= $lang ?>">
<? 
$iid = $iFrom;
foreach ($SENTENCES as $s) {
?>
	<li id="s<?= $iid ?>"><p class="sentence original l<?= $lang ?>"><?= $s['sentence'] ?></p><div class="metadata"><?= $s['metadata'] ?></div></li>
<? 
	$iid++;
} ?>
</ol>

<? if (array_key_exists("annotation", $_REQUEST)) {
	 $curPage = $_SERVER["REQUEST_URI"];
	 $m = array();
	 preg_match('/([-]?[0-9]+)/', $curPage, $m);
	 $pageIndex = $m[1];
	 $nextPage = preg_replace('/[-]?[0-9]+/', ''.(intval($pageIndex)+1), $curPage, 1);
?>
<div class="instructions">
<p><a href="<?= $nextPage ?>">next page</a></p>
</div>
<? } ?>
<div class="controls">
<p>
	<input type="hidden" name="assignmentId" value="<?= $_REQUEST['assignmentId'] ?>" />
	<input type="hidden" name="from" value="<?= ($iFrom+$perpage) ?>" />
	<input type="hidden" name="to" value="<?= $iTo ?>" />
	<input type="hidden" name="resultsLog" id="results" value="" />
	<input type="hidden" name="resultsTags" id="resultsTags" value="" />
</p>

<p style="text-align: center;"><input type="submit" id="btnSubmit" name="submit" /></p>

</div>
</form>
</body>
</html>